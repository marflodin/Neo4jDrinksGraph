package com.marflo.drinkgraph.parser;

import javax.xml.parsers.*;

import com.marflo.drinkgraph.data.ArticleEntity;
import org.joda.money.Money;
import org.neo4j.driver.v1.*;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ParseSystembolagetCsv {

    static final String XML_LOCATION = "target/classes/csv/systembolaget-produkter.xml";
    static int counter = 0;

    public static void main(String[] args) throws Exception {
        Long startTime = new Date().getTime();
        List<ArticleEntity> articles = new ArrayList<>();
        Document dom;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        DocumentBuilder db = dbf.newDocumentBuilder();
        dom = db.parse(XML_LOCATION);

        Element doc = dom.getDocumentElement();
        NodeList nl = doc.getChildNodes();
        for (int i = 0; i < 100 /*nl.getLength()*/; i++) {
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) nl.item(i);
                if (el.getNodeName().contains("artikel")) {
                    articles.add(parseArtikel(el));
                }
            }
        }
        System.out.println("num: " + articles.size());
        createNeo4jNodes(articles);
        Long endTime = new Date().getTime();
        System.out.println("done in[ms]: " + (endTime - startTime));
    }

    private static ArticleEntity parseArtikel(Element el) {
        ArticleEntity article = new ArticleEntity();

        article.setArticleId(Long.valueOf(getElementByName(el, "Artikelid")));
        article.setName(getElementByName(el, "Namn"));
        article.setName2(getElementByName(el, "Namn2"));
        article.setPrice(Money.parse("SEK " + getElementByName(el, "Prisinklmoms")));
        article.setVolumeInMl(Integer.valueOf(getElementByName(el, "Volymiml")));
        article.setSalesStart(getElementByName(el, "Saljstart"));
        article.setAlcoholPercentage(Integer.valueOf(getElementByName(el, "Alkoholhalt").replace("%", ""))*100);

        article.setAssortment(getElementByName(el, "Sortiment"));
        article.setArticleType(getElementByName(el, "Varugrupp"));
        article.setCountry(getElementByName(el, "Ursprunglandnamn"));
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

    private static void createNeo4jNodes(List<ArticleEntity> articles) {
        //start neo4j session
        Driver driver = GraphDatabase.driver("bolt://localhost", AuthTokens.basic("neo4j", "test123"));
        Session session = driver.session();
        System.out.println("start!");
        //Loop over all articles
        for (ArticleEntity article : articles) {
            session.run("MERGE (a:Article {id: {articleId}, name:{name}, " +
                            "name2:{name2}, price:{price}, volume: {volumeInMl}, " +
                            "salesStart: {salesStart}, alcoholPercentage: {alcoholPercentage} })",
                    Values.parameters("articleId", article.getArticleId(),
                            "name", article.getName(),
                            "name2", article.getName2(),
                            "price", article.getPrice().getAmountMinorInt(),
                            "volumeInMl", article.getVolumeInMl(),
                            "salesStart", article.getSalesStart(),
                            "alcoholPercentage", article.getAlcoholPercentage()
                    ));

            if (article.getAssortment() != null && !article.getAssortment().isEmpty()) {
                session.run("MERGE (b:Assortment {name: {name} })",
                        Values.parameters("name", article.getAssortment() ));

                session.run("MATCH (a:Article) WHERE a.id = {articleId} " +
                                "MATCH (b:Assortment) WHERE b.name = {assortmentName} " +
                                "MERGE (a)-[:ASSORTMENT]->(b)",
                        Values.parameters("articleId", article.getArticleId(),
                                "assortmentName", article.getAssortment()));
            }

            if (article.getArticleType() != null && !article.getArticleType().isEmpty()) {
                session.run("MERGE (b:ArticleType {name: {name} })",
                        Values.parameters("name", article.getArticleType() ));

                session.run("MATCH (a:Article) WHERE a.id = {articleId} " +
                                "MATCH (b:ArticleType) WHERE b.name = {articleTypeName} " +
                                "MERGE (a)-[:TYPE]->(b)",
                        Values.parameters("articleId", article.getArticleId(),
                                "articleTypeName", article.getArticleType()));
            }

            if (article.getCountry() != null && !article.getCountry().isEmpty()) {
                session.run("MERGE (b:Country {name: {name} })",
                        Values.parameters("name", article.getCountry() ));

                session.run("MATCH (a:Article) WHERE a.id = {articleId} " +
                                "MATCH (b:Country) WHERE b.name = {countryName} " +
                                "MERGE (a)-[:FROM_COUNTRY]->(b)",
                        Values.parameters("articleId", article.getArticleId(),
                                "countryName", article.getCountry()));
            }

            if (article.getYear() != null) {
                session.run("MERGE (b:Year {name: {name} })",
                        Values.parameters("name", article.getYear() ));

                session.run("MATCH (a:Article) WHERE a.id = {articleId} " +
                                "MATCH (b:Year) WHERE b.name = {yearName} " +
                                "MERGE (a)-[:FROM_YEAR]->(b)",
                        Values.parameters("articleId", article.getArticleId(),
                                "yearName", article.getYear()));
            }

            if (article.getPackaging() != null && !article.getPackaging().isEmpty()) {
                session.run("MERGE (b:Package {name: {name} })",
                        Values.parameters("name", article.getPackaging() ));

                session.run("MATCH (a:Article) WHERE a.id = {articleId} " +
                                "MATCH (b:Package) WHERE b.name = {packageName} " +
                                "MERGE (a)-[:PACKAGING]->(b)",
                        Values.parameters("articleId", article.getArticleId(),
                                "packageName", article.getPackaging()));
            }

            if (article.getProducer() != null && !article.getProducer().isEmpty()) {
                session.run("MERGE (b:Producer {name: {name} })",
                        Values.parameters("name", article.getProducer() ));

                session.run("MATCH (a:Article) WHERE a.id = {articleId} " +
                                "MATCH (b:Producer) WHERE b.name = {producerName} " +
                                "MERGE (a)-[:FROM_PRODUCER]->(b)",
                        Values.parameters("articleId", article.getArticleId(),
                                "producerName", article.getProducer()));
            }

            if (article.getSupplier() != null && !article.getSupplier().isEmpty()) {
                session.run("MERGE (b:Supplier {name: {name} })",
                        Values.parameters("name", article.getSupplier() ));

                session.run("MATCH (a:Article) WHERE a.id = {articleId} " +
                                "MATCH (b:Supplier) WHERE b.name = {supplierName} " +
                                "MERGE (a)-[:FROM_SUPPLIER]->(b)",
                        Values.parameters("articleId", article.getArticleId(),
                                "supplierName", article.getSupplier()));
            }

            counter++;
            if (counter % 1000 == 0)
                System.out.println("count: " + counter);
        }


        //close neo4j session
        System.out.println("done!");
        session.close();
        driver.close();
    }
}
