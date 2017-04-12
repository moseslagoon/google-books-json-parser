package API;

import Books_and_Inventories.Book;
import Queries.Strategy;
import com.google.gson.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * JSONParserBooks class.
 * Books are only considered available for purchase via the LBMS if the
 * "saleability" is "FOR_SALE" and in the "country" is "US."
 * Created by Moses Lagoon
 */
public class JSONParserBooks implements Strategy{

    /**
     * Main Class for testing the JSONParser.
     * */
    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {

        ArrayList<Book> books = makeBooksFromJSON();
        System.out.println("Google Books: " + books);
        System.out.println("Size of Google Books " + books.size());

        // Give the handleSearch() a search array

        //String sTerm = handleSearch()
       // System.out.println(getJSONResponse("The Alchemist"));


        /**
         * Parse the file,
         * Implement the search functionality under GoogleBooksAPI
         * Make the search connection with the GUI
         * */

    }

    /**
    * Parse the JSON Books, create a book out of response and return the
     * arrayList containing all the books from the search
    * */
    public static ArrayList<Book> makeBooksFromJSON() throws
            IOException,
            ParserConfigurationException, SAXException {

        ArrayList<Book> GoogleBooks = new ArrayList<>();
        String isbn = "";
        String title = null;
        //ArrayList<String> authorsArray = new ArrayList<>();
        String publisher = "";
        String pubDate = null;             // Need this in localDateTime
        LocalDateTime publishedDate;
        int pageCount = 0;
        JsonArray authors = null;
        String isbnType=null;

        // Get Response takes a parameter here.
        String searchTeam = "The Alchemist";
        String jsonResponse = getJSONResponse(searchTeam);

        // Create  new Json Instance
        JsonParser parser = new JsonParser();
        JsonElement content = parser.parse(jsonResponse);
        JsonElement items = content.getAsJsonObject().get("items");

        // JsonArray something = items.getAsJsonArray();
        // System.out.println("ITEMS: " + items);
        JsonArray itemsAsJsonArray = items.getAsJsonArray();

        // Loop through the items as JSON array to grab each element and put
        // them into their respective objects, and hold them into variables.

        for (int i = 0; i < itemsAsJsonArray.size() - 1; i++) {
            JsonElement element = itemsAsJsonArray.get(i);
//             Only get the books with  saleability as "FOR_SALE" and country as
//             "US"

            //get the volumeinfo of that particular element;
            JsonElement volumeInfo = element.getAsJsonObject().get
                    ("volumeInfo");

            //get the title of the book
            if (volumeInfo.getAsJsonObject().get("title") != null) {
                title = volumeInfo.getAsJsonObject().get("title").getAsString();
            }

            //get the authors
            if (volumeInfo.getAsJsonObject().get("authors") != null) {
                authors = volumeInfo.getAsJsonObject().get
                        ("authors").getAsJsonArray();
            }
            ArrayList<String> authorsArray = new ArrayList<>();
            for (JsonElement author : authors) {
                authorsArray.add(author.getAsString());
            }
            // get the publisher

            if (volumeInfo.getAsJsonObject().get("publisher") != null) {
                publisher = volumeInfo.getAsJsonObject().get
                        ("publisher").getAsString();
            }
            // get the publishedDate
            if (volumeInfo.getAsJsonObject().get("publishedDate") != null) {
                pubDate = volumeInfo.getAsJsonObject().get("publishedDate")
                        .getAsString();
            }

            // get the pageCount
            if (volumeInfo.getAsJsonObject().get("pageCount") != null) {
                pageCount = volumeInfo.getAsJsonObject().get("pageCount")
                        .getAsInt();
            }

            // get the ISBN_10 specifically, if its not 10, don't add it
            // Loop through the arrayList, check that the type = "ISBN_10"
            if (volumeInfo.getAsJsonObject().get("industryIdentifiers") != null) {
                JsonArray industryIdentifiers = volumeInfo.getAsJsonObject
                        ().get("industryIdentifiers").getAsJsonArray();

                for(int j = 0; j < industryIdentifiers.size(); j++){
                    JsonElement idTypes = industryIdentifiers.get(j);
                    isbnType = idTypes.getAsJsonObject().get
                            ("type").getAsString();
                    if(isbnType.equals("ISBN_10")){
                        isbn = idTypes.getAsJsonObject().get("identifier")
                                .getAsString();
                    }
                }
            }

            // Take String pubDate and parse it into LocalDateTime
            String pubDateString = pubDate.replace("*", "");
            String[] parameters = pubDateString.split("-");
            if (parameters.length == 1) {
                publishedDate = LocalDateTime.parse(parameters[0] +
                        "-01-01T12:00:00");
            } else if (parameters.length == 2)
                publishedDate = LocalDateTime.parse(parameters[0] + "-" +
                        parameters[1] + "-01T12:00:00");
            else
                publishedDate = LocalDateTime.parse(parameters[0] + "-" +
                        parameters[1] + "-" + parameters[2] + "T12:00:00");

            // Check for country and saleability
            JsonElement saleInfo = element.getAsJsonObject().get("saleInfo");
            String country = saleInfo.getAsJsonObject().get("country").getAsString();
            String saleability = saleInfo.getAsJsonObject().get("saleability")
                    .getAsString();

            // ONly create the book and add to the GoogleBooks array if the
            // saleability is FOR_SALE and COUNTRY=US

            // Check for saleability to be "FOR_SALE" and Country "US".
            // Exclude books with ISBN_TYPE as "OTHERS"
            if (saleability.equals("FOR_SALE") && country.equals("US") &&
                    !isbnType.equals("OTHER")) {
                Book gBook = new Book(isbn, title, authorsArray, publisher,
                        publishedDate, pageCount);
                GoogleBooks.add(gBook);

            }
//            /**
//             *
//             * Pretty Printing the Book list
//             *
//             */
            System.out.println(i + " " +country + " "+ saleability + " " +
                            isbnType +
                    " Title:" +
                    " " +
                    title +
                    " Authors: " + authors + " Publisher : " + publisher + " " +
                    " PubDate: " + publishedDate + " PageCount : " + pageCount);

        }
        return GoogleBooks;
    }


    /**
     * Handles search, method handles the search as required by the user.
     * (search: String) -> (ArrayList<object>
     *     array.get(0): String - it could be title or '*'
     *          if it's title -> return the books with that title
     *          otherwise: * means return everything --> Blank url?
     *
     *     Array[1] --> array.get(1): ArrayList<String> --> authors or *
     *     Array[2] --> ISBN : String
     *     Array[3] --> Publisher? : String
     *
     * */

    public static String handleSearch(ArrayList<Object> searchArray){
        String searchTerm = "";
        String getTitle = (String) searchArray.get(0);
        String getAuthors = (String) searchArray.get(1);
        String getISBN = (String) searchArray.get(2);
        String getPublisher = (String) searchArray.get(3);


        if(getTitle.equals("*")){
            searchTerm = "search+terms";
        }else{
            searchTerm = getTitle;     //title would be
            // the search term here
        }
        if(getAuthors.equals("*")){
            searchTerm += "";
        }else{
            searchTerm += getAuthors;
        }
        searchTerm += getISBN;
        searchTerm += getPublisher;
        return searchTerm;
    }

    /**
     * Fetches the required JSONResponse according to searchTerm parameters:
     * title, author, saleability,etc
     * */

    public static String getJSONResponse(String searchTerm) throws IOException {

        String url = "https://www.googleapis" +
                ".com/books/v1/volumes?q="+searchTerm.replaceAll(" ", "%20");

        System.out.println(url);          //Just printing out the url

        // Create a URL and open a connection
        URL GoogleBooksURL = new URL(url);
        HttpURLConnection urlConnection = (HttpURLConnection) GoogleBooksURL.openConnection();

        urlConnection.setRequestMethod("GET");
        urlConnection.setConnectTimeout(10000);
        urlConnection.setReadTimeout(10000);

        // Created a BufferedReader to read the contents of the request
        BufferedReader in = new BufferedReader(new InputStreamReader
                (urlConnection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while((inputLine = in.readLine()) != null){
            response.append(inputLine);
        }
        // MAKE SURE TO CLOSE YOUR CONNECTION;
        in.close();
        urlConnection.disconnect();

        //System.out.println(response.toString());
        String jsonLine = response.toString();
        return jsonLine;

    }
