package tech.muiru.ncba.soap.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import tech.muiru.ncba.dto.CountryInfoDTO;
import tech.muiru.ncba.dto.LanguageDTO;
import tech.muiru.ncba.exception.CustomException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CountryInfoSoapClient {

    private final RestTemplate restTemplate;

    @Value("${soap.endpoint}")
    private String soapEndpoint;

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000L;

    public String getCountryISOCode(String countryName) {
        log.info("getCountryISOCode - countryName: {}", countryName);
        String requestXml = buildIsoCodeRequest(countryName);

        String response = executeWithRetry(requestXml, "getCountryISOCode");
        String isoCode = parseIsoCodeResponse(response);

        log.info("getCountryISOCode - result isoCode: {}", isoCode);
        return isoCode;
    }

    public CountryInfoDTO getFullCountryInfo(String isoCode) {
        log.info("getFullCountryInfo - isoCode: {}", isoCode);
        String requestXml = buildFullCountryInfoRequest(isoCode);

        String response = executeWithRetry(requestXml, "getFullCountryInfo");
        CountryInfoDTO dto = parseFullCountryInfoResponse(response, isoCode);

        log.info("getFullCountryInfo - result country: {}", dto.name());
        return dto;
    }

    private String executeWithRetry(String requestXml, String operationName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        HttpEntity<String> request = new HttpEntity<>(requestXml, headers);

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("{} - attempt: {}", operationName, attempt);
                ResponseEntity<String> response = restTemplate.postForEntity(soapEndpoint, request, String.class);
                return response.getBody();
            } catch (Exception ex) {
                lastException = ex;
                log.info("{} - attempt: {} failed, error: {}", operationName, attempt, ex.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new CustomException("SOAP call interrupted: " + operationName);
                    }
                }
            }
        }
        throw new CustomException("SOAP call failed after " + MAX_RETRIES + " attempts for: " + operationName + " - " + lastException.getMessage());
    }

    private String buildIsoCodeRequest(String countryName) {
        return """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <web:CountryISOCode xmlns:web="http://www.oorsprong.org/websamples.countryinfo">
                      <web:sCountryName>%s</web:sCountryName>
                    </web:CountryISOCode>
                  </soap:Body>
                </soap:Envelope>
                """.formatted(countryName);
    }

    private String buildFullCountryInfoRequest(String isoCode) {
        return """
                <?xml version="1.0" encoding="utf-8"?>
                <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                  <soap:Body>
                    <web:FullCountryInfo xmlns:web="http://www.oorsprong.org/websamples.countryinfo">
                      <web:sCountryISOCode>%s</web:sCountryISOCode>
                    </web:FullCountryInfo>
                  </soap:Body>
                </soap:Envelope>
                """.formatted(isoCode);
    }

    private String parseIsoCodeResponse(String responseXml) {
        try {
            Document doc = parseXml(responseXml);
            NodeList nodes = doc.getElementsByTagNameNS("*", "CountryISOCodeResult");
            if (nodes.getLength() == 0) {
                throw new CustomException("CountryISOCodeResult element not found in SOAP response");
            }
            String result = nodes.item(0).getTextContent().trim();
            if (result.isEmpty()) {
                throw new CustomException("Country ISO code result is empty - country may not exist");
            }
            return result;
        } catch (CustomException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CustomException("Failed to parse ISO code response: " + ex.getMessage(), ex);
        }
    }

    private CountryInfoDTO parseFullCountryInfoResponse(String responseXml, String isoCode) {
        try {
            Document doc = parseXml(responseXml);
            NodeList resultNodes = doc.getElementsByTagNameNS("*", "FullCountryInfoResult");
            if (resultNodes.getLength() == 0) {
                throw new CustomException("FullCountryInfoResult element not found in SOAP response");
            }
            Element result = (Element) resultNodes.item(0);

            String name = getElementText(result, "sName");
            String capitalCity = getElementText(result, "sCapitalCity");
            String phoneCode = getElementText(result, "sPhoneCode");
            String continentCode = getElementText(result, "sContinentCode");
            String currencyIsoCode = getElementText(result, "sCurrencyISOCode");
            String countryFlag = getElementText(result, "sCountryFlag");

            List<LanguageDTO> languages = parseLanguages(result);

            return CountryInfoDTO.builder()
                    .name(name)
                    .capitalCity(capitalCity)
                    .phoneCode(phoneCode)
                    .continentCode(continentCode)
                    .currencyIsoCode(currencyIsoCode)
                    .countryFlag(countryFlag)
                    .isoCode(isoCode)
                    .languages(languages)
                    .build();
        } catch (CustomException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CustomException("Failed to parse full country info response: " + ex.getMessage(), ex);
        }
    }

    private List<LanguageDTO> parseLanguages(Element result) {
        List<LanguageDTO> languages = new ArrayList<>();
        NodeList languageNodes = result.getElementsByTagNameNS("*", "tLanguage");
        for (int i = 0; i < languageNodes.getLength(); i++) {
            Element lang = (Element) languageNodes.item(i);
            String langIsoCode = getElementText(lang, "sISOCode");
            String langName = getElementText(lang, "sName");
            languages.add(LanguageDTO.builder().isoCode(langIsoCode).name(langName).build());
        }
        return languages;
    }

    private String getElementText(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
