package com.marflo.drinkgraph.parser;

import com.marflo.drinkgraph.data.ArticleEntity;
import org.joda.money.Money;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ParseInformationToCsv {

    private static final String XML_LOCATION = "target/classes/csv/systembolaget-produkter.xml";
    private static final String COMMA_DELIMITER = ",";
    private static final String NEW_LINE_SEPARATOR = "\n";

    public static void main(String[] args) throws Exception {

        List<ArticleEntity> articles = new ArrayList<>();
        Set<String> assortments = new HashSet<>();
        Set<String> articleTypes = new HashSet<>();
        Set<String> countries = new HashSet<>();
        Set<String> areas = new HashSet<>();
        Set<String> years = new HashSet<>();
        List<String> relationshipRows = new ArrayList<>();

        Document dom;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        dom = db.parse(XML_LOCATION);

        Element doc = dom.getDocumentElement();
        NodeList nl = doc.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) nl.item(i);
                if (el.getNodeName().contains("artikel")) {
                    articles.add(parseArtikel(el));
                }
            }
        }
        System.out.println("num: " + articles.size());

        for(ArticleEntity article : articles) {
            if (article.getAssortment() != null && !article.getAssortment().isEmpty()) {
                assortments.add(article.getAssortment());
                relationshipRows.add(addRelationship(article.getArticleId().toString(),
                        article.getAssortment(), "ASSORTMENT"));
            }
            if (article.getArticleType() != null && !article.getArticleType().isEmpty()) {
                articleTypes.add(article.getArticleType());
                relationshipRows.add(addRelationship(article.getArticleId().toString(),
                        article.getArticleType(), "TYPE"));
            }
            if (article.getCountry() != null && !article.getCountry().isEmpty()) {
                countries.add(article.getCountry());
                relationshipRows.add(addRelationship(article.getArticleId().toString(),
                        article.getCountry(), "FROM_COUNTRY"));
            }
            if (article.getArea() != null && !article.getArea().isEmpty()) {
                areas.add(article.getArea());
                relationshipRows.add(addRelationship(article.getArticleId().toString(),
                        article.getArea(), "FROM_AREA"));
            }
            if (article.getYear() != null) {
                years.add(article.getYear().toString());
                relationshipRows.add(addRelationship(article.getArticleId().toString(),
                        article.getYear().toString(), "FROM_YEAR"));
            }
        }

        createArticlesNodes(articles);
        createSimpleNodesFromSet(assortments, "assortment_nodes.csv", "assortment:ID,:LABEL", "assortment");
        createSimpleNodesFromSet(articleTypes, "articleType_nodes.csv", "articleType:ID,:LABEL", "articleType");
        createSimpleNodesFromSet(countries, "country_nodes.csv", "country:ID,:LABEL", "country");
        createSimpleNodesFromSet(areas, "area_nodes.csv", "area:ID,:LABEL", "area");
        createSimpleNodesFromSet(years, "year_nodes.csv", "year:ID,:LABEL", "year");
        createRelationships(relationshipRows);
        System.out.println("done");
    }



    private static String addRelationship(String startNode, String endNode, String type) {
        return startNode + "," + "\"" + endNode + "\"" + "," + type;
    }

    private static void createArticlesNodes(List<ArticleEntity> articles) {
        final String ARTICLES_HEADER = "article:ID,name1,name2,price,volume,salesStart,alcoholPercentage,:LABEL";
        final String ARTICLES_LABEL = "article";
        FileWriter writer = null;
        try {
            writer = new FileWriter("C:\\Users\\Martin\\Documents\\neo4j-import\\systembolaget\\article_nodes.csv");

            writer.append(ARTICLES_HEADER);
            writer.append(NEW_LINE_SEPARATOR);
            for (ArticleEntity article : articles) {
                writer.append(article.getArticleId().toString());
                writer.append(COMMA_DELIMITER);
                writer.append("\"" + article.getName() + "\"");
                writer.append(COMMA_DELIMITER);
                writer.append("\"" + article.getName2() + "\"");
                writer.append(COMMA_DELIMITER);
                writer.append(article.getPrice().toString());
                writer.append(COMMA_DELIMITER);
                writer.append(article.getVolumeInMl().toString());
                writer.append(COMMA_DELIMITER);
                writer.append(article.getSalesStart());
                writer.append(COMMA_DELIMITER);
                writer.append(article.getAlcoholPercentage().toString());
                writer.append(COMMA_DELIMITER);

                writer.append(ARTICLES_LABEL);
                writer.append(NEW_LINE_SEPARATOR);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }
    }

    private static void createSimpleNodesFromSet(Set<String> set, String fileName, String header, String label) {
        FileWriter writer = null;
        try {
            writer = new FileWriter("C:\\Users\\Martin\\Documents\\neo4j-import\\systembolaget\\" + fileName);

            writer.append(header);
            writer.append(NEW_LINE_SEPARATOR);
            for (String entry: set) {
                writer.append("\"" +entry + "\"");
                writer.append(COMMA_DELIMITER);
                writer.append(label);
                writer.append(NEW_LINE_SEPARATOR);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }
    }

    private static void createRelationships(List<String> relationshipRows) {
        final String ARTICLES_HEADER = ":START_ID,:END_ID,:TYPE";
        FileWriter writer = null;
        try {
            writer = new FileWriter("C:\\Users\\Martin\\Documents\\neo4j-import\\systembolaget\\relationships.csv");

            writer.append(ARTICLES_HEADER);
            writer.append(NEW_LINE_SEPARATOR);
            for (String relationshipRow: relationshipRows) {
                writer.append(relationshipRow);
                writer.append(NEW_LINE_SEPARATOR);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                System.out.println("Error while flushing/closing fileWriter !!!");
                e.printStackTrace();
            }
        }
    }


    private static ArticleEntity parseArtikel(Element el) {
        ArticleEntity article = new ArticleEntity();

        article.setArticleId(Long.valueOf(getElementByName(el, "Artikelid")));
        article.setName(getElementByName(el, "Namn"));
        article.setName2(getElementByName(el, "Namn2"));
        article.setPrice(Money.parse("SEK " + getElementByName(el, "Prisinklmoms")));
        article.setVolumeInMl(Double.parseDouble(getElementByName(el, "Volymiml")));
        article.setSalesStart(getElementByName(el, "Saljstart"));
        article.setAlcoholPercentage(Double.parseDouble(getElementByName(el, "Alkoholhalt").replace("%", "")));

        article.setAssortment(getElementByName(el, "Sortiment"));
        article.setArticleType(getElementByName(el, "Varugrupp"));
        article.setCountry(getElementByName(el, "Ursprunglandnamn"));
        article.setArea(getElementByName(el, "Ursprung"));
        try {
            article.setYear(Integer.valueOf(getElementByName(el, "Argang")));
        } catch (NumberFormatException e) {
            //don't set if not a number
        }
        article.setPackaging(getElementByName(el, "Forpackning"));
        article.setSupplier(getElementByName(el, "Leverantor"));
        article.setProducer(getElementByName(el, "Producent"));
        return article;
    }

    private static String getElementByName(Element el, String tagName) {
        try {
            return el.getElementsByTagName(tagName).item(0).getTextContent().trim();
        } catch (Exception e) {
            return "";
        }
    }
}
