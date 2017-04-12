package API;

import Books_and_Inventories.Book;
import Books_and_Inventories.BookCopy;
import Queries.Strategy;
import com.google.gson.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * JSONParserBooks class.
 * Books are only considered available for purchase via the LBMS if the
 * "saleability" is "FOR_SALE" and in the "country" is "US."
 * Written by Moses Lagoon
 */
public class JSONParserBooks implements Strategy{
    private String jsonLine= "";            // Stores JSONResponse from
                                            // runReport to be parsed
    private String sortOrder="";            // sortOrder

    /**
     * Main Class in class for testing purposes.
     * */
    public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {

        JSONParserBooks parser = new JSONParserBooks();
        parser.runReport(null, null);
        ArrayList<Object> result = parser.getResults();
        System.out.println("RESULT: " + result);

    }


    /**
     * Takes in a ArrayList of search parameters as an argument and returns the
     * required searchTerm by the GoogleAPI URL to request.
     * @param searchArray ArrayList of search parameters
     * @return searchTerm required by the GoogleAPI url.
     */
    public String handleSearch(ArrayList<Object> searchArray){

        String searchTerm = "";
        // Get the required data from the searchArray;
        // Parse the ArrayList<Object> parameters [*, [*], *, *, *] for the
        // required search term
        // Get the title
        String searchTitle = (String) searchArray.get(0);
        // Get the authors
        ArrayList<String> searchAuthors = (ArrayList<String>) searchArray
                .get(1);
        // Get the ISBN
        String searchISBN = (String) searchArray.get(2);
        // Get the Publisher
        String searchPublisher = (String) searchArray.get(3);
        sortOrder = (String) searchArray.get(4);

        // SearchTitle
        if (!searchTitle.equals("*")){
            searchTerm += "intitle:" + searchTitle+ "+";
        }

        // Search Authors Term retrieval
        String authors = "";
        for(int i = 0; i < searchAuthors.size(); i++){
            authors += searchAuthors.get(i);
        }

        if (!authors.equals("*")){
            searchTerm += "inauthor:"+authors;
        }
        // Search ISBN Term retrieval
        if (!searchISBN.equals("*")){
            searchTerm += "isbn:"+searchISBN;
        }

        // Search Publisher Term retrieval
        if (!searchPublisher.equals("*")){
            searchTerm += "inpublisher:" + searchPublisher;
        }

        // In case of no search term entered -> [*, [*], *, *, *] or search,*;
        if(searchTerm.equals(""))
            searchTerm += "search+terms";

        // Give the required searchTerm as entered
        return searchTerm;
    }


    /**
     * Fetches the required JSONResponse according to searchTerm parameters:
     * title, author, saleability,etc
     * */
    @Override
    public void runReport(Object target, ArrayList<Object> parameters) {
        // Get the searchTerm to search with
        String searchTerm = handleSearch(parameters);

        String url = "https://www.googleapis" +
                ".com/books/v1/volumes?q="+searchTerm.replaceAll(" ", "%20");
        System.out.println(url);

        // Create a URL and open a connection
        URL GoogleBooksURL = null;
        try {
            GoogleBooksURL = new URL(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) GoogleBooksURL.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            urlConnection.setRequestMethod("GET");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
        urlConnection.setConnectTimeout(10000);
        urlConnection.setReadTimeout(10000);

        // Created a BufferedReader to read the contents of the request
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader
                    (urlConnection.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String inputLine;
        StringBuilder response = new StringBuilder();
        try {
            while((inputLine = in.readLine()) != null){
                response.append(inputLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // MAKE SURE TO CLOSE YOUR CONNECTION;
        try {
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        urlConnection.disconnect();
        System.out.println(response.toString());
        this.jsonLine = response.toString();      // save the response to
                                                 // jsonLine for use in parsing
    }

    /**
     * Parse the JSON Books, create a book out of response and return the
     * arrayList containing all the books from the search
     * */
    @Override
    public ArrayList<Object> getResults() {
        ArrayList<Book> GoogleBooks = new ArrayList<>();
        String isbn = "";
        String title = null;
        String publisher = "";
        String pubDate = null;
        LocalDateTime publishedDate;
        int pageCount = 0;
        JsonArray authors = null;
        String isbnType=null;


        // Create  new Json Instance
        JsonParser parser = new JsonParser();
        JsonElement content = parser.parse(jsonLine);

        // Make sure totalItems != 0 otherwise return an empty ArrayList in
        // the end
        int totalItems = content.getAsJsonObject().get("totalItems")
                .getAsInt();
        if(totalItems > 0){
            JsonElement items = content.getAsJsonObject().get("items");
            JsonArray itemsAsJsonArray = items.getAsJsonArray();

            // Go through the items array to grab each element and put
            // them into their respective objects, and hold them into variables.
            for (int i = 0; i < itemsAsJsonArray.size(); i++) {
                JsonElement element = itemsAsJsonArray.get(i);

                //get the volumeUnfo of that particular element;
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
                // Go through the arrayList, check that the type = "ISBN_10"
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

                // Only Books with saleability="FOR_SALE" and Country="US"
                // qualify
                // Check for saleability to be "FOR_SALE" and Country "US".
                // Exclude books with ISBN_TYPE as "OTHERS"
                if (saleability.equals("FOR_SALE") && country.equals("US") &&
                        !isbnType.equals("OTHER")) {
                    Book gBook = new Book(isbn, title, authorsArray, publisher,
                            publishedDate, pageCount);
                    GoogleBooks.add(gBook);

                }
            /**
             *
             * Pretty Printing the Book list
             *

                System.out.println(i + " " +country + " "+ saleability + " " +
                        isbnType +
                        " Title:" +
                        " " +
                        title +
                        " Authors: " + authors + " Publisher : " + publisher + " " +
                        " PubDate: " + publishedDate + " PageCount : " + pageCount);
             **/
            }
            ArrayList<Object> results = new ArrayList<>();
            results.add(GoogleBooks);

            // Handling sort - order by title, publish-date and book-status;
            if (!sortOrder.equals("*")) {
                // Alphanumerical [0..9-A..Z]
                if(sortOrder.equals("title"))
                    GoogleBooks.sort(this::alphanumericSort);
                // Published-date [newest first]
                if(sortOrder.equals("publish-date"))
                    GoogleBooks.sort(this::publishedDateSort);
                // Book-status [Most Copies first]
                if(sortOrder.equals("book-status")) {
                    GoogleBooks.sort(this::bookStatusSort);
                    GoogleBooks.removeIf(book -> {
                        int availableCopyCount = 0;

                        for (BookCopy copy : book.getBookCopies()) {
                            if (copy.getStateOfCopy() == 0)
                                availableCopyCount++;
                        }

                        return availableCopyCount == 0;
                    });
                }
            }
            // Return the sorted books list.

            results.addAll(GoogleBooks);
            return results;

        // An empty arrayList is returned if no books found to be qualified
        }else{
            ArrayList<Object> results = new ArrayList<>();
            results.add(new ArrayList<>());
            return results;
        }
    }

    ///////////////////////////////////////////////////////////////////
    //          Sort Algorithms                                      //
    //////////////////////////////////////////////////////////////////

    private int alphanumericSort(Book book1, Book book2) {
        return book1.getTitle().compareTo(book2.getTitle());
    }

    private int publishedDateSort(Book book1, Book book2) {
        return book2.getPubDate().compareTo(book1.getPubDate());
    }

    private int bookStatusSort(Book book1, Book book2) {
        // Store the amount of copies available for both books
        int counter1 = 0;
        int counter2 = 0;

        // Loop through each Book and figure out the number of copies not checked out
        for (BookCopy copy : book1.getBookCopies()) {
            if (copy.getStateOfCopy() == 0)
                counter1++;
        }

        for (BookCopy copy : book2.getBookCopies())
            if (copy.getStateOfCopy() == 0)
                counter2++;

        // Return [-1 for less than] 0 for equal [1 for greater than]
        if (counter1 < counter2)
            return -1;
        else {
            if (counter1 > counter2)
                return 1;
            else
                return 0;
        }
    }
}
