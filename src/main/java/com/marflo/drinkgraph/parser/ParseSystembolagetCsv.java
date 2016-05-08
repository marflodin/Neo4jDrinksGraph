package com.marflo.drinkgraph.parser;

import javax.xml.parsers.*;

import com.marflo.drinkgraph.data.ArticleEntity;
import com.marflo.drinkgraph.data.PriceClasses;
import org.joda.money.Money;
import org.neo4j.driver.v1.*;
import org.w3c.dom.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ParseSystembolagetCsv {

    static final String XML_LOCATION = "target/classes/csv/systembolaget-produkter.xml";
    static int counter = 0;
    static List<PriceClasses> priceClasses;

    public static void main(String[] args) throws Exception {
        Long startTime = new Date().getTime();
        List<ArticleEntity> articles = new ArrayList<>();
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

    private static void createNeo4jNodes(List<ArticleEntity> articles) {
        //start neo4j session
        Driver driver = GraphDatabase.driver("bolt://localhost", AuthTokens.basic("neo4j", "test123"));
        Session session = driver.session();
        System.out.println("start!");
        constructPriceClasses(session);
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

            if (article.getArea() != null && !article.getArea().isEmpty()) {
                session.run("MERGE (b:Area {name: {name} })",
                        Values.parameters("name", article.getArea()));

                session.run("MATCH (a:Article) WHERE a.id = {articleId} " +
                                "MATCH (b:Area) WHERE b.name = {areaName} " +
                                "MERGE (a)-[:FROM_AREA]->(b)",
                        Values.parameters("articleId", article.getArticleId(),
                                "areaName", article.getArea()));

                if (article.getCountry() != null && !article.getCountry().isEmpty()) {
                    session.run("MATCH (a:Country) WHERE a.name = {countryName} " +
                                    "MATCH (b:Area) WHERE b.name = {areaName} " +
                                    "MERGE (b)-[:PART_OF]->(a)",
                            Values.parameters("countryName", article.getCountry(),
                                    "areaName", article.getArea()));
                }
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

            mapToPriceClass(article, session);

            counter++;
            if (counter % 1000 == 0)
                System.out.println("count: " + counter);
        }


        //close neo4j session
        System.out.println("done!");
        session.close();
        driver.close();
    }

    private static void mapToPriceClass(ArticleEntity article, Session session) {
        Integer amountInMinor = article.getPrice().getAmountMinorInt();
        for (PriceClasses priceClass : priceClasses) {
            if (priceClass.getMinPrice() <= amountInMinor &&
                    priceClass.getMaxPrice() > amountInMinor)
                session.run("MATCH (a:Article) WHERE a.id = {articleId} " +
                                "MATCH (b:PriceClass) WHERE b.name = {priceClassName} " +
                                "MERGE (a)-[:IN_PRICE_RANGE]->(b)",
                        Values.parameters("articleId", article.getArticleId(),
                                "priceClassName", priceClass.getName()));
        }
    }

    private static void constructPriceClasses(Session session) {
        priceClasses = new ArrayList<>();
        priceClasses.add(new PriceClasses("0-20 SEK", 0, 2000));
        priceClasses.add(new PriceClasses("20-100 SEK", 2000, 10000));
        priceClasses.add(new PriceClasses("100-200 SEK", 10000, 20000));
        priceClasses.add(new PriceClasses("200-300 SEK", 20000, 30000));
        priceClasses.add(new PriceClasses("300-600 SEK", 30000, 60000));
        priceClasses.add(new PriceClasses("600+ SEK", 60000, Integer.MAX_VALUE));

        for (PriceClasses priceClass : priceClasses) {
            session.run("MERGE (a:PriceClass {name: {name}, minPrice:{minPrice}, maxPrice: {maxPrice} })",
                    Values.parameters("name", priceClass.getName(),
                            "minPrice", priceClass.getMinPrice(),
                            "maxPrice", priceClass.getMaxPrice()
                            ));
        }
    }
}
