package com.dove.dart.application;

import com.dove.dart.application.dto.CorpMapping;
import com.dove.dart.config.DartProperties;
import com.dove.workspace.WorkArea;
import com.dove.workspace.Workspace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * DART 고유번호 매핑(corpCode.xml, ZIP)을 내려받아 상장사 매핑 목록으로 파싱한다.
 */
@Component
@RequiredArgsConstructor
public class CorpCodeDownloader {

    private static final String SCOPE = "dart-corpcode";

    private final DartProperties properties;
    private final Workspace workspace;

    /**
     * 종목코드가 있는 상장사만 (고유번호, 회사명, 종목코드)로 반환한다.
     * ZIP은 작업 폴더에 내려받아 파싱 후 자동 삭제된다.
     *
     * @throws DartCorpCodeException 다운로드·파싱 실패 시
     */
    public List<CorpMapping> download() {
        try (WorkArea area = workspace.open(SCOPE)) {
            Path zipPath = area.resolve("corpCode.zip");
            fetchZip(zipPath);
            byte[] xml = firstEntry(zipPath);
            return parse(xml);
        } catch (Exception e) {
            throw new DartCorpCodeException("DART_CORPCODE_FAILED", e);
        }
    }

    private void fetchZip(Path target) throws Exception {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getBaseUrl() + "/corpCode.xml?crtfc_key=" + properties.getApiKey()))
                .timeout(Duration.ofSeconds(60))
                .GET().build();
        http.send(request, HttpResponse.BodyHandlers.ofFile(target));
    }

    private byte[] firstEntry(Path zipPath) throws Exception {
        try (InputStream in = Files.newInputStream(zipPath);
             ZipInputStream zis = new ZipInputStream(in)) {
            if (zis.getNextEntry() == null) {
                throw new IllegalStateException("EMPTY_ZIP");
            }
            return zis.readAllBytes();
        }
    }

    private List<CorpMapping> parse(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        NodeList nodes = doc.getElementsByTagName("list");
        List<CorpMapping> out = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element el = (Element) node;
            String stockCode = text(el, "stock_code");
            if (stockCode == null || stockCode.isBlank()) {
                continue;
            }
            out.add(new CorpMapping(text(el, "corp_code"), text(el, "corp_name"), stockCode.trim()));
        }
        return out;
    }

    private static String text(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        return nl.getLength() > 0 ? nl.item(0).getTextContent() : null;
    }
}
