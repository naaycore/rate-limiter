package com.prodigious;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.TextNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Decoder {
    public static void decode(String url) throws IOException {
        List<String> nodeStrings =
                retrieveTableContent(url);

        List<CharacterCoordinates> characterCoordinates =
                computeCharacterCoordinates(nodeStrings);
        char[][] grid = createGrid(characterCoordinates);
        printGrid(grid);
    }

    public static List<String> retrieveTableContent(String url)
            throws IOException {
        Document document = Jsoup.connect(url).get();

        return document
                .body()
                .select("table")
                .select("tbody")
                .select("tr")
                .select("td").select("span")
                .textNodes()
                .stream().map(TextNode::toString).toList();
    }

    private static List<CharacterCoordinates> computeCharacterCoordinates(
            List<String> nodes
    ) {
        List<CharacterCoordinates> coords = new ArrayList<>();
        for (int i = 3; i < nodes.size(); i += 3) {
            CharacterCoordinates coord = compute(nodes, i);
            coords.add(coord);
        }
        return coords;
    }

    private static char[][] createGrid(
            List<CharacterCoordinates> characterCoordinates
    ) {
        int maxX = 0;
        int maxY = 0;

        for (int i = 0; i < characterCoordinates.size(); i++) {
            maxX = Math.max(characterCoordinates.get(i).xCoord, maxX);
            maxY = Math.max(characterCoordinates.get(i).yCoord, maxY);
        }

        char[][] grid = new char[maxX + 1][maxY + 1];

        characterCoordinates
                .forEach(c ->
                                 grid[c.xCoord][c.yCoord] = c.ch);

        return grid;
    }

    private static void printGrid(char[][] grid){
        for(int i = grid[0].length - 1 ; i >= 0; i--) {
            for (int j = 0; j < grid.length; j++) {
                System.out.print(grid[j][i]);
            }
            System.out.print(System.lineSeparator());
        }
    }

    private static CharacterCoordinates compute(
            List<String> nodes,
            int index
    ) {
        int x = Integer.parseInt(nodes.get(index));
        char ch = nodes.get(index + 1).charAt(0);
        int y = Integer.parseInt(nodes.get(index + 2));
        return new CharacterCoordinates(x, y, ch);
    }

    private record CharacterCoordinates(int xCoord, int yCoord, char ch) {
    }

    static void main() throws IOException {
        decode("https://docs.google.com/document/d/e/2PACX-1vRPzbNQcx5UriHSbZ-9vmsTow_R6RRe7eyAU60xIF9Dlz-vaHiHNO2TKgDi7jy4ZpTpNqM7EvEcfr_p/pub");
    }
}
