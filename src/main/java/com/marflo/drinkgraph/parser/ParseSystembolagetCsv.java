package com.marflo.drinkgraph.parser;

import javax.xml.parsers.*;

import com.marflo.drinkgraph.data.ArticleEntity;
import org.joda.money.Money;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.neo4j.driver.v1.*;
import org.w3c.dom.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ParseSystembolagetCsv {

    static final String XML_LOCATION = "target/classes/csv/systembolaget-produkter.xml";
    static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd");

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
        article.setVolumeInMl(new BigDecimal(getElementByName(el, "Volymiml")));
        article.setSalesStart(formatter.parseDateTime(getElementByName(el, "Saljstart")).toDate());
        article.setAlcoholPercentage(new BigDecimal(getElementByName(el, "Alkoholhalt").replace("%", "")));

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

        //Loop over all articles
        for (ArticleEntity article : articles) {
            session.run("CREATE (a:Article {id: {articleId}, name:{name}, " +
                            "name2:{name2}, price:{price}, volume: {volumeInMl}, " +
                            "salesStart: {salesStart}, alcoholPercentage: {alcoholPercentage} })",
                    Values.parameters("articleId", article.getArticleId(),
                            "name", article.getName(),
                            "name2", article.getName2(),
                            "price", article.getPrice().toString(),
                            "volumeInMl", article.getVolumeInMl().toString(),
                            "salesStart", article.getSalesStart().toString(),
                            "alcoholPercentage", article.getAlcoholPercentage().toString()
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

        }

        //Create ingredients nodes
        /*for (String ingredient : ingredients) {
            session.run("MERGE (a:Ingredient {name: {ingredientName}})",
                    Values.parameters("ingredientName", ingredient));
        }*/

        //Create mappings
        /*for (Map.Entry<String, List<IngredientEntity>> drinkIngredientConnection : drinkIngredientsMapping.entrySet()) {
            for (IngredientEntity ingredient: drinkIngredientConnection.getValue()) {

                session.run("MATCH (d:Drink) WHERE d.name = {drinkName} " +
                                "MATCH (i:Ingredient) WHERE i.name = {ingredientName} " +
                                "MERGE (d)-[:CONTAINS]->(i)",
                        Values.parameters("drinkName", drinkIngredientConnection.getKey(),
                                "ingredientName", ingredient.getName()));
            }
        }*/

        //close neo4j session
        System.out.println("done!");
        session.close();
        driver.close();
    }

}
