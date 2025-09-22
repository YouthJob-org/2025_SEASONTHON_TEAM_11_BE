package com.youthjob.api.job.service;

import com.youthjob.api.job.domain.JobPosting;
import com.youthjob.api.job.dto.Enriched;
import com.youthjob.api.job.repository.JobPostingRepository;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class Work24CrawlerService {

    private final JobPostingRepository repo;


    // 엔드포인트
    private static final String BASE = "https://www.work24.go.kr";
    private static final String LIST_URL = BASE + "/wk/a/b/1200/retriveDtlEmpSrchList.do";
    private static final String DETAIL_PATH = "/wk/a/b/1500/empDetail.do";
    private static final String AUTH_VIEW_PATH = "/wk/a/b/1500/empDetailAuthView.do"; // ← 추가

    // href에서 상세경로 매칭 (AuthView 포함)
    private static final Pattern DETAIL_HREF_RE =
            Pattern.compile("(?i)/(?:empDetailAuthView|empDetail|wantedEmpDetail|empWantedDetail)\\.do\\b");


    // ── Time/format
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DOT  = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DASH = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter SLASH= DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter YMD  = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ── Paging
    private static final int PAGE_SIZE = 50;
    private static final int MAX_PAGES_HARD = 3000;
    private static final Pattern ATTR_KEY_RE =
            Pattern.compile("(?i)wanted.*auth.*no|seq.*no|emp.*seq.*no");
    private static final Pattern KV_WANTED_RE =
            Pattern.compile("(?i)(?:wantedAuthNo|wanted_auth_no|wanted-auth-no)\\s*[:=]\\s*['\"]?([A-Za-z]\\d{6,})");
    private static final Pattern RAW_K_RE =
            Pattern.compile("(?i)\\bK\\d{10,}\\b"); // K로 시작하는 긴 공고번호


    private static final Pattern ANY_FN_RE      = Pattern.compile("(?i)([a-zA-Z_]\\w*)\\s*\\(([^)]*)\\)");
    private static final Set<String> FN_DENY = Set.of(
            "resultdatetemplate","resultregtemplate","gopaging","gosearch",
            "openlayer","closelayer","openmodal","closemodal",
            "share","scrap","favorite","bookmark","mapview","print"
    );
    private static final String[] FN_HINTS = {"detail","dtl","wanted","recruit","emp"};
    private static final Set<String> ALLOWED_KEYS = Set.of("seqNo","empSeqNo","wantedAuthNo","wantedno","empWantedno","recruitNo");

    // ── 텍스트 정규식
    private static final Pattern TOTAL_RE = Pattern.compile("검색건수\\s*([0-9,]+)건");
    private static final Pattern DOT_DATE_RE   = Pattern.compile("\\b(\\d{4}\\.\\d{2}\\.\\d{2})\\b");
    private static final Pattern DASH_DATE_RE  = Pattern.compile("\\b(\\d{4}-\\d{2}-\\d{2})\\b");
    private static final Pattern SLASH_DATE_RE = Pattern.compile("\\b(\\d{4}/\\d{2}/\\d{2})\\b");
    private static final Pattern BAD_TITLE_RE  = Pattern.compile("지도 ?보기|길찾기|센터\\s*지도|오시는 ?길");

    // 상세 라벨
    private static final String RX_COMPANY  = "기업명|회사명|상호|법인명|사업장명|기관명|회사\\(기관\\)명|사업체명|업체명|기관\\s*명";
    private static final String RX_REGION = "근무지역|근무지(?:\\s*주소)?|근무장소|근무\\s*위치|근무지주소|지역|근무\\s*예정지|주소";
    private static final String RX_EMPLOY   = "고용형태|근무형태(?:/시간)?";
    // 공백/NBSP 공통 패턴
    private static final String WS = "[\\s\\u00A0]*";

    // 급여 라벨: "임금 조건", "급여 조건" 등 변형까지 허용
    private static final String RX_SALARY =
            "임금" + WS + "조건|급여" + WS + "조건|임금|급여|연봉|월급|시급|일급";
    private static final String RX_POSTED   = "등록일|게시일|게재일|공고시작|모집시작일";
    private static final String RX_DEADLINE = "마감일|접수마감|공고마감|모집마감일";

    private static final Pattern COMPANY_IN_TEXT = Pattern.compile(
            "(?:주식회사|\\(주\\)|유한회사|재단법인|사단법인|사회복지법인|학교|대학교|병원|의원|협회|센터|공단|공사)?\\s*([가-힣A-Za-z0-9&·\\s]{2,40})"
    );

    // 목록에서 뽑은 힌트
    @Value private static class Link {
        String url;
        String titleHint;
        String companyHint;
    }


    @Transactional
    public int crawlAndSave() throws Exception {
        LocalDate now = LocalDate.now(KST);
        LocalDate regFrom = now.minusMonths(1), regTo = now;
        LocalDate ddlFrom = now, ddlTo = now.plusMonths(1);

        int saved = 0;
        Set<String> seen = new LinkedHashSet<>();

        Document first = fetchList(1, regFrom, regTo, ddlFrom, ddlTo);
        if (first == null) return 0;

        int total = parseTotalCount(first);
        int totalPages = (total > 0) ? Math.min((total + PAGE_SIZE - 1) / PAGE_SIZE, MAX_PAGES_HARD) : MAX_PAGES_HARD;
        log.info("total={}, pageSize={}, totalPages={}", total, PAGE_SIZE, totalPages);

        for (int page = 1; page <= totalPages; page++) {
            Document list = (page == 1) ? first : fetchList(page, regFrom, regTo, ddlFrom, ddlTo);
            if (list == null) break;

            List<Link> links = extractDetailLinks(list);
            log.info("page {} detailUrls={}", page, links.size());
            if (links.isEmpty()) {
                if (page <= 3) dump(list, "work24_list_p" + page + ".html");
                if (page == 1) log.warn("상세 링크를 한 개도 못 찾음");
                break;
            }

            int newThisPage = 0;
            for (Link link : links) {
                String externalId = sha256(link.getUrl());
                if (!seen.add(externalId)) continue;

                // 상세 파싱 (실패 시 null)
                Enriched e = fetchDetail(link.getUrl());

                // 실패했더라도 목록 힌트로 최소 저장
                if (e == null) {
                    e = new Enriched(
                            n(link.getTitleHint()),
                            n(link.getCompanyHint()),
                            null, null, null, null,
                            null
                    );
                    log.debug("detail parse failed. fallback with hints. url={}", link.getUrl());
                }

                // 기간 필터
                LocalDate regDate = e.getRegDate();
                LocalDate deadline = e.getDeadline();
                if (regDate != null && regDate.isBefore(regFrom)) {
                    log.debug("skip: regDate {} < {}", regDate, regFrom);
                    continue;
                }
                if (deadline != null && deadline.isAfter(ddlTo)) {
                    log.debug("skip: deadline {} > {}", deadline, ddlTo);
                    continue;
                }

                String title = e.getTitle();
                if (title != null && BAD_TITLE_RE.matcher(title).find()) {
                    log.debug("skip: bad title '{}'", title);
                    continue;
                }

                JobPosting job = repo.findByExternalId(externalId)
                        .orElseGet(() -> JobPosting.builder().externalId(externalId).build());
                job.setDetailUrl(link.getUrl());

                // 회사/제목은 상세→히트 순서로 보강
                final int REGION_MAX = 255; // DB 컬럼 길이와 맞추기

                job.setTitle( coalesce(e.getTitle(), link.getTitleHint()) );
                job.setCompany( coalesce(e.getCompany(), link.getCompanyHint()) );

                String cleanedRegion = clamp(cleanRegion(e.getRegion()), REGION_MAX);
                job.setRegion(cleanedRegion);

                job.setEmploymentType( n(e.getEmploymentType()) );
                job.setSalary( n(e.getSalary()) );
                job.setRegDate(regDate);
                job.setDeadline(deadline);


                repo.save(job);
                saved++; newThisPage++;
                Thread.sleep(120);
            }

            if (newThisPage == 0) break;
            Thread.sleep(250);
        }

        log.info("total saved={}", saved);
        return saved;
    }

    // ── 목록 요청
    private Document fetchList(int page, LocalDate regFrom, LocalDate regTo, LocalDate ddlFrom, LocalDate ddlTo) {
        try {
            Connection conn = Jsoup.connect(LIST_URL)
                    .userAgent("Mozilla/5.0 (compatible; YouthJobBot/1.0)")
                    .referrer(LIST_URL)
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .timeout(15000)
                    .method(Connection.Method.GET)
                    .data("searchMode","Y")
                    .data("siteClcd","all")
                    .data("empTpGbcd","1")
                    .data("currentPageNo", String.valueOf(page))
                    .data("pageIndex", String.valueOf(page))
                    .data("resultCnt", String.valueOf(PAGE_SIZE))
                    .data("sortField","DATE")
                    .data("sortOrderBy","DESC")
                    .data("termSearchGbn","M-1")
                    .data("regDateStdtParam", regFrom.format(YMD))
                    .data("regDateEndtParam", regTo.format(YMD))
                    .data("cloTermSearchGbn","M-1")
                    .data("cloDateStdtParam", ddlFrom.format(YMD))
                    .data("cloDateEndtParam", ddlTo.format(YMD))
                    .data("moreButtonYn","Y");

            Document doc = conn.get();
            log.info("list p{} title='{}' len={}", page, doc.title(), doc.outerHtml().length());
            return doc;
        } catch (Exception e) {
            log.warn("fetchList p{} error: {}", page, e.toString());
            return null;
        }
    }

    private int parseTotalCount(Document doc) {
        Matcher m = TOTAL_RE.matcher(doc.text());
        if (m.find()) {
            try { return Integer.parseInt(m.group(1).replace(",", "")); }
            catch (Exception ignore) {}
        }
        return -1;
    }

    // ── 목록에서 상세 링크 + 힌트 뽑기
    // 목록에서 상세 링크 + 힌트 뽑기 (교체)
    private List<Link> extractDetailLinks(Document doc) {
        LinkedHashSet<Link> out = new LinkedHashSet<>();

        // 1) href에 상세경로가 직접 있는 경우
        for (Element a : doc.select("a[href]")) {
            String href = a.attr("href");
            if (href == null || href.isBlank()) continue;

            String url = null;
            if (DETAIL_HREF_RE.matcher(href).find()) {
                String abs = a.absUrl("href");
                url = (abs != null && !abs.isBlank()) ? abs : (BASE + href);
            } else if (href.startsWith("javascript:")) {
                url = buildDetailUrlFromJs(href.substring("javascript:".length()));
            }
            if (url != null) {
                // wantedAuthNo가 있으면 어떤 경로든 AuthView로 정규화(+필수 파라미터 보장)
                if (url.contains("wantedAuthNo=")) {
                    int idx = url.indexOf('?');
                    String q = (idx >= 0) ? url.substring(idx) : "";
                    url = toAuthView(q);
                }
                String[] hints = listHints(a);
                out.add(new Link(url, hints[0], hints[1]));
            }
        }

        // 2) onclick 기반
        for (Element el : doc.select("[onclick]")) {
            String js = el.attr("onclick");
            if (js == null || js.isBlank()) continue;
            String url = buildDetailUrlFromJs(js);
            if (url != null) {
                String[] hints = listHints(el);
                out.add(new Link(url, hints[0], hints[1]));
            }
        }

        // 3) data-* 속성에서 wantedAuthNo/seqNo 복원
        for (Element el : doc.getAllElements()) {
            el.attributes().forEach(attr -> {
                String key = attr.getKey();
                String val = attr.getValue();
                if (key == null || val == null) return;
                if (!ATTR_KEY_RE.matcher(key).find()) return;

                // 값 안에서 K공고번호/숫자 추출 시도
                Matcher mk = RAW_K_RE.matcher(val);
                if (mk.find()) {
                    out.add(new Link(toAuthView("?wantedAuthNo=" + mk.group().toUpperCase()), null, null));
                    return;
                }
                // key=value류 문자열에서 wantedAuthNo 추출
                Matcher m = KV_WANTED_RE.matcher(val);
                if (m.find()) {
                    out.add(new Link(toAuthView("?wantedAuthNo=" + m.group(1).toUpperCase()), null, null));
                    return;
                }
                // digits만 있는 seqNo 형태
                String digits = val.replaceAll("\\D+", "");
                if (digits.length() >= 4 && key.toLowerCase().contains("seq")) {
                    out.add(new Link(BASE + DETAIL_PATH + "?seqNo=" + digits, null, null));
                }
            });
        }

        // 4) <script>나 전체 HTML에서 wantedAuthNo/K숫자 스캔 (최후 폴백)
        String html = doc.outerHtml();
        Matcher m1 = KV_WANTED_RE.matcher(html);
        while (m1.find()) {
            out.add(new Link(toAuthView("?wantedAuthNo=" + m1.group(1).toUpperCase()), null, null));
        }
        Matcher m2 = RAW_K_RE.matcher(html);
        while (m2.find()) {
            out.add(new Link(toAuthView("?wantedAuthNo=" + m2.group().toUpperCase()), null, null));
        }

        // 로깅/검증
        if (out.isEmpty()) {
            log.warn("extractDetailLinks(): 0 url, href={}, onclick={}",
                    doc.select("a[href]").size(), doc.select("[onclick]").size());
        } else {
            out.stream().limit(5).forEach(l -> log.info("detail sample: {}", l.getUrl()));
        }
        return new ArrayList<>(out);
    }


    // 목록 행/카드에서 제목/회사 힌트 추출
    private String[] listHints(Element el) {
        String titleHint = null, companyHint = null;

        // 같은 행의 컬럼 추정
        Element row = el.closest("tr");
        if (row != null) {
            List<Element> tds = row.select("td");
            if (tds.size() >= 2) {
                // 사이트 컬럼 순서: [회사명 | 채용공고명 | ...] 인 경우가 많음
                companyHint = n(textSafe(tds.get(0)));
                titleHint   = n(textSafe(tds.get(1)));
            }
        }
        // 카드형
        if (titleHint == null)   titleHint   = pickFirstIn(el, ".title, .tit, a.tit, a.title, dt.tit a");
        if (companyHint == null) companyHint = pickFirstIn(el, ".company, .corp, .cpn, .name");

        // 앵커 자신
        if (titleHint == null) titleHint = n(el.text());

        return new String[]{ n(titleHint), n(companyHint) };
    }

    private String pickFirstIn(Element base, String css) {
        Element x = base.closest("li, div, tr");
        if (x == null) x = base.parent();
        if (x == null) return null;
        Element t = x.selectFirst(css);
        return (t == null) ? null : n(t.text());
    }


    private Enriched fetchDetail(String url) {
        try {
            Document d = fetchDetailDoc(url);   // ← 단일 URL이 아니라 후보들을 시도
            if (d == null) return null;

            String ttl = d.title();
            if (ttl != null && (ttl.contains("로그인") || ttl.contains("오류") || ttl.contains("접근권한"))) {
                log.debug("detail looks like login/error page: {}", ttl);
                return null;
            }

            String title   = pickFirst(d, "meta[property=og:title]", "content",
                    ".job-title", ".title", "h2.title", "h3.title");
            String company = extractCompany(d, title);
            String region  = pickByLabel(d, RX_REGION);
            if (isBlank(region)) region = pickInlineByEm(d, RX_REGION);  // <em class="tit">지역</em> 케이스
            if (isBlank(region)) region = pickFirstCss(d,
                    "th:matchesOwn(^\\s*근무\\s*예정지\\s*$) + td", // 표 형태
                    ".addr", ".workplace", ".region"               // ← '.location' 제거!
            );
            String empType = pickByLabel(d, RX_EMPLOY);
            if (isBlank(empType)) empType = pickInlineByEm(d, RX_EMPLOY);  // ⬅️ 추가
            if (isBlank(empType)) empType = pickFirstCss(d, ".employment", ".emp-type");
            String salary  = pickByLabel(d, RX_SALARY);

            LocalDate posted   = parseAnyDate(pickByLabel(d, RX_POSTED));
            LocalDate deadline = parseAnyDate(pickByLabel(d, RX_DEADLINE));

            if (isBlank(region))  region  = pickFirstCss(d, ".addr", ".workplace", ".region");
            if (isBlank(empType)) empType = pickFirstCss(d, ".employment", ".emp-type");
            if (isBlank(salary))  salary  = pickFirstCss(d, ".pay", ".salary", ".wage");
            if (posted == null)   posted  = parseAnyDate(d.text());
            if (deadline == null) deadline= parseAnyDate(findNear(d, "마감|마감일|접수마감"));

            return new Enriched(n(title), n(company), n(region), n(empType), n(salary), posted, deadline);
        } catch (Exception e) {
            log.debug("detail fetch fail {} -> {}", url, e.toString());
            return null;
        }
    }


    // ── JS 호출문 → 상세 URL
    private String buildDetailUrlFromJs(String jsCall) {
        Matcher m = ANY_FN_RE.matcher(jsCall);
        if (!m.find()) return null;
        String fn = m.group(1);
        String args = m.group(2);

        String fnLow = fn.toLowerCase(Locale.ROOT);
        if (FN_DENY.contains(fnLow)) return null;
        boolean looksDetail = Arrays.stream(FN_HINTS).anyMatch(fnLow::contains);
        if (!looksDetail) return null;

        Map<String,String> p = parseArgs(args);
        if (p.isEmpty() || !hasValidId(p)) return null;

        String q = toQuery(p);

        // ← 여기 보강: wantedAuthNo 뿐 아니라 wantedno/empWantedno 또는 값 자체가 K…면 AuthView로
        boolean hasWantedParam =
                p.containsKey("wantedAuthNo") || p.containsKey("wantedno") || p.containsKey("empWantedno") ||
                        p.values().stream().anyMatch(v -> v != null && v.matches("(?i)[A-Z]\\d{6,}"));

        String url = hasWantedParam ? toAuthView("?" + q)
                : (BASE + DETAIL_PATH + "?" + q);

        log.debug("onclick -> {} -> {}", fn, url);
        return url;
    }


    private Map<String,String> parseArgs(String raw) {
        Map<String,String> out = new LinkedHashMap<>();
        if (raw == null) return out;
        String s = raw.trim();
        if ((s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\"")))
            s = s.substring(1, s.length()-1);

        if (s.contains("=") && s.contains("&")) {
            for (String kv : s.split("&")) {
                String[] a = kv.split("=", 2);
                if (a.length == 2 && ALLOWED_KEYS.contains(a[0])) out.put(a[0], strip(a[1]));
            }
            return out;
        }

        if (s.matches("\\d{4,}")) { out.put("seqNo", s); return out; }

        String[] tok = s.split(",");
        List<String> vals = new ArrayList<>();
        for (String t : tok) {
            String v = strip(t);
            if (!v.isBlank()) vals.add(v);
        }
        if (vals.size() >= 2) {
            boolean pairs = vals.size() % 2 == 0 && vals.get(0).matches("[A-Za-z].*");
            if (pairs) {
                for (int i = 0; i + 1 < vals.size(); i += 2) {
                    String k = vals.get(i), v = vals.get(i+1);
                    if (ALLOWED_KEYS.contains(k)) out.put(k, v);
                }
            } else if (vals.get(0).matches("\\d{4,}")) {
                out.put("seqNo", vals.get(0));
                if (vals.size() > 1 && vals.get(1).matches("[A-Za-z]\\d{3,}")) {
                    out.put("wantedAuthNo", vals.get(1));
                }
            }
        }
        return out;
    }

    private boolean hasValidId(Map<String,String> p) {
        for (String v : p.values()) {
            if (v == null) continue;
            if (v.matches("\\d{4,}") || v.matches("[A-Za-z]\\d{3,}")) return true;
        }
        return false;
    }
    // 전화번호 간단 정규화: +82 → 0, 숫자만 남기고 02/010 등 형태로 하이픈 삽입
    private String normalizePhone(String s) {
        if (s == null) return null;
        String raw = s.trim();
        if (raw.isEmpty()) return null;

        // 괄호/내선 등의 부가정보를 거칠게 제거
        raw = raw.replaceAll("\\(.*?\\)", " ");
        raw = raw.replaceAll("내선\\s*\\d+", " ");

        // +82 → 0 으로 변환
        String tmp = raw.replaceAll("\\s+", "");
        if (tmp.startsWith("+82")) tmp = "0" + tmp.substring(3);

        // 숫자만 추출
        String digits = tmp.replaceAll("[^0-9]", "");
        if (digits.length() < 8) return raw; // 유효 길이 아니면 원본 반환

        // 서울(02)
        if (digits.startsWith("02")) {
            if (digits.length() == 9)  return "02-" + digits.substring(2,5) + "-" + digits.substring(5);
            if (digits.length() == 10) return "02-" + digits.substring(2,6) + "-" + digits.substring(6);
        }
        // 휴대폰/일반 지역번호
        if (digits.length() == 11) return digits.substring(0,3) + "-" + digits.substring(3,7) + "-" + digits.substring(7);
        if (digits.length() == 10) return digits.substring(0,3) + "-" + digits.substring(3,6) + "-" + digits.substring(6);
        if (digits.length() == 8)  return digits.substring(0,4) + "-" + digits.substring(4); // 4-4

        // 다른 경우는 원본 유지
        return raw;
    }

    // ── Label/CSS helpers
    private String pickByLabel(Document d, String rx) {
        Element td = d.selectFirst("th:matchesOwn(^\\s*(?:" + rx + ")\\s*$) + td");
        if (td != null && !td.text().isBlank()) return td.text().trim();
        Element dd = d.selectFirst("dt:matchesOwn(^\\s*(?:" + rx + ")\\s*$) + dd");
        if (dd != null && !dd.text().isBlank()) return dd.text().trim();
        return null;
    }

    private String extractCompany(Document d, String titleFallback) {
        String c = pickByLabel(d, RX_COMPANY);
        if (isBlank(c)) {
            Element s = d.selectFirst(".emp_sumup_wrp .corp_info > strong, .corp_info > strong");
            if (s != null && !s.text().isBlank()) c = s.text().trim();  // ex) (주)대유산업
        }

        // ③ 기업정보 탭의 인라인 라벨( <em class="tit">기업명</em>… )
        if (isBlank(c)) c = pickInlineByEm(d, RX_COMPANY);

        // ④ 나머지 기존 폴백 유지 …
        if (isBlank(c)) c = pickFirstCss(d, ".company", ".cpn", ".employer", ".biz-name", ".co_name");
        if (isBlank(c)) {
            String pageTitle = d.title();
            if (!isBlank(pageTitle)) {
                Matcher m = COMPANY_IN_TEXT.matcher(pageTitle.replace("채용"," ").replace("공고"," "));
                if (m.find()) c = m.group(1).trim();
            }
        }
        if (isBlank(c)) {
            Matcher m = COMPANY_IN_TEXT.matcher(d.text());
            if (m.find()) c = m.group(1).trim();
        }
        if (isBlank(c) && !isBlank(titleFallback)) {
            Matcher m = COMPANY_IN_TEXT.matcher(titleFallback);
            if (m.find()) c = m.group(1).trim();
        }
        if (!isBlank(c)) {
            String s = c.replaceAll("\\s+"," ").trim();
            if (s.contains("고용센터") || s.endsWith(" 지도") || s.endsWith(" 지도 보기")) return null;
            if (s.length() > 40 && s.contains("채용")) s = s.split("채용")[0].trim();
            return s;
        }
        return null;
    }

    private String pickFirst(Document d, String css, String attrIfMeta, String... moreCss) {
        Element e = d.selectFirst(css);
        if (e != null) {
            if ("meta".equalsIgnoreCase(e.tagName()) && attrIfMeta != null && e.hasAttr(attrIfMeta))
                return e.attr(attrIfMeta).trim();
            return e.text().trim();
        }
        for (String s : moreCss) {
            e = d.selectFirst(s);
            if (e != null && !e.text().isBlank()) return e.text().trim();
        }
        return null;
    }

    private String pickFirstCss(Document d, String... selectors) {
        for (String s : selectors) {
            Element e = d.selectFirst(s);
            if (e != null && !e.text().isBlank()) return e.text().trim();
        }
        return null;
    }

    private String findNear(Document d, String labelRegex) {
        for (Element el : d.select("th, dt, label, strong, b, span")) {
            if (el.text().matches(".*(" + labelRegex + ").*")) {
                Element sib = el.nextElementSibling();
                if (sib != null && !sib.text().isBlank()) return sib.text();
            }
        }
        return null;
    }

    // ── 날짜 파서: dot/dash/slash 모두 허용
    private LocalDate parseAnyDate(String txt) {
        if (txt == null) return null;
        Matcher m;
        m = DOT_DATE_RE.matcher(txt);
        if (m.find()) try { return LocalDate.parse(m.group(1), DOT); } catch (Exception ignore) {}
        m = DASH_DATE_RE.matcher(txt);
        if (m.find()) try { return LocalDate.parse(m.group(1), DASH);} catch (Exception ignore) {}
        m = SLASH_DATE_RE.matcher(txt);
        if (m.find()) try { return LocalDate.parse(m.group(1), SLASH);} catch (Exception ignore) {}
        return null;
    }

    // ── misc
    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return Integer.toHexString(s.hashCode()); }
    }
    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
    private static String n(String s) { return isBlank(s) ? null : s; }
    private static String coalesce(String a, String b) { return n(a) != null ? n(a) : n(b); }
    private static String textSafe(Element e) { return e == null ? null : e.text(); }
    private static String strip(String s) {
        String x = s.trim();
        if ((x.startsWith("'") && x.endsWith("'")) || (x.startsWith("\"") && x.endsWith("\"")))
            x = x.substring(1, x.length()-1);
        return x.replaceAll("\\s+","");
    }
    private static String toQuery(Map<String,String> p) {
        StringBuilder sb = new StringBuilder();
        p.forEach((k,v)->{ if (sb.length()>0) sb.append('&'); sb.append(k).append('=').append(v);});
        return sb.toString();
    }
    private void dump(Document d, String filename) {
        try {
            Path path = Path.of(System.getProperty("java.io.tmpdir"), filename);
            Files.writeString(path, d.outerHtml());
            log.info("dumped: {}", path);
        } catch (Exception ignore) {}
    }
    private Document fetchDetailDoc(String url) throws Exception {
        List<String> candidates = buildDetailUrlCandidates(url);

        for (String u : candidates) {
            Connection.Response res = Jsoup.connect(u)
                    .userAgent("Mozilla/5.0 (compatible; YouthJobBot/1.0)")
                    .referrer(LIST_URL)
                    .header("Accept-Language", "ko-KR,ko;q=0.9")
                    .maxBodySize(0)
                    .timeout(15000)
                    .ignoreHttpErrors(true)
                    .execute();

            int sc = res.statusCode();
            if (sc == 200) {
                if (!u.equals(url)) log.debug("detail alt matched -> {}", u);
                return res.parse();
            } else {
                log.debug("detail non-200 {} -> {}", sc, u);
            }
        }
        return null;
    }
    // 3) 원본 URL을 바탕으로 대체 후보들을 만드는 헬퍼 추가
    private List<String> buildDetailUrlCandidates(String url) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        set.add(url);

        int qIdx = url.indexOf('?');
        String q = (qIdx >= 0) ? url.substring(qIdx) : "";

        if (!q.isEmpty() && q.contains("wantedAuthNo=")) {
            // 정답 경로: empDetailAuthView.do + 기본 params
            set.add(toAuthView(q)); // ← 최우선 후보

            // 혹시 모를 과거 라우트도 여전히 시도(보조)
            set.add(BASE + "/wk/a/b/1500/wantedEmpDetail.do" + q);
            set.add(BASE + "/wk/a/b/1500/empWantedDetail.do" + q);
            set.add(BASE + "/wk/a/b/1500/EmpWantedDetail.do" + q);
            set.add(BASE + DETAIL_PATH + q);
        } else if (!q.isEmpty() && (q.contains("seqNo=") || q.contains("empSeqNo="))) {
            set.add(BASE + DETAIL_PATH + q);
        }
        return new ArrayList<>(set);
    }

    private String ensureParam(String url, String k, String v) {
        if (url.contains(k + "=")) return url;
        return url + (url.contains("?") ? "&" : "?") + k + "=" + v;
    }
    private String toAuthView(String q) {
        String u = BASE + AUTH_VIEW_PATH + (q.startsWith("?") ? q : "?" + q);
        u = ensureParam(u, "infoTypeCd", "VALIDATION");
        u = ensureParam(u, "infoTypeGroup", "tb_workinfoworknet");
        return u;
    }

    private String pickInlineByEm(Document d, String rx) {
        Element li = d.selectFirst("li:has(> em.tit:matchesOwn(^\\s*(?:" + rx + ")\\s*$))");
        if (li == null) return null;
        Element p = li.selectFirst("p");
        String text = (p != null ? p.text() : li.ownText());
        if (text != null) {
            return text.replaceFirst("^\\s*(?:" + rx + ")\\s*", "").trim();
        }
        return null;
    }
    // 지역 정리 (불필요 텍스트 제거)
    private String cleanRegion(String s) {
        if (s == null) return null;
        String r = s
                .replaceAll("홈\\s*채용정보.*?일자리\\s*찾기", "") // 빵크럼 제거
                .replaceAll("지도\\s*보기|길찾기|로그인", "")
                .replaceAll("\\s+", " ")
                .trim();
        return r.isEmpty() ? null : r;
    }

    // 길이 클램프
    private String clamp(String s, int max) {
        if (s == null) return null;
        s = s.trim();
        return (s.length() <= max) ? s : s.substring(0, max);
    }

}
