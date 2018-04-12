package netfondsrepos.downloader;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import oahu.annotations.StoreHtmlPage;
import oahu.annotations.StoreTxtPage;
import oahu.financial.html.EtradeDownloader;
import oahu.financial.html.WebClientManager;

import java.io.IOException;

public class DefaultDownloader implements EtradeDownloader<Page> {


    //webClient.getOptions().setJavaScriptEnabled(false);
 
   // private WebClient webClient;

    //private boolean javaScriptEnabled = false;

    public DefaultDownloader() {
        /*
        webClient = new WebClient();
        webClient.getOptions().setJavaScriptEnabled(isJavaScriptEnabled());
        */
    }

    /*
    public void setJavaScriptEnabled(boolean value) {
        javaScriptEnabled = value;
        webClient.getOptions().setJavaScriptEnabled(value);
    }
    public boolean isJavaScriptEnabled() {
        return javaScriptEnabled;
    }
    */

    private WebClientManager webClientManager;


    /*
    HtmlPage loginPage = null;
    HtmlPage logoutPage = null;
    @Override
    public void login() throws IOException {
        Optional<Tuple<String>> prm = logonParam();
        if (prm.isPresent()) {

            Tuple<String> prmx = prm.get();


            HtmlPage page = webClient.getPage("https://bang.netfonds.no/auth.php");
            HtmlForm form = page.getFormByName("login");

            HtmlSubmitInput button = form.getInputByName("confirm");
            HtmlTextInput customer = form.getInputByName("customer");
            HtmlPasswordInput password = form.getInputByName("password");

            customer.setValueAttribute(prmx.first());
            password.setValueAttribute(prmx.second());

            loginPage = button.click();
        }
    }
    
    @Override
    public void logout() throws IOException {
        String logoutAnchor = "https://www.netfonds.no/account/logout.php";
        HtmlAnchor logout = loginPage.getAnchorByHref(logoutAnchor);
        logoutPage = logout.click();
    }
    */
    @Override
    @StoreHtmlPage
    public Page downloadDerivatives(String ticker) throws IOException {
        return webClientManager.getPage(tickerUrl(ticker));
    }

    @Override
    @StoreHtmlPage
    public Page downloadIndex(String stockIndex) throws IOException {
        return webClientManager.getPage(indexUrl(stockIndex));
    }

    @Override
    @StoreTxtPage
    public Page downloadPaperHistory(String ticker) throws IOException {
        return webClientManager.getPage(paperHistoryUrl(ticker));
    }
    @Override
    public Page downloadDepth(String ticker) throws  IOException {
        return webClientManager.getPage(depthUrl(ticker));
    }
    @Override
    public Page downloadPurchases(String ticker) throws  IOException {
        return webClientManager.getPage(purchasesUrl(ticker));
    }

    private String paperHistoryUrl(String ticker) {
        return String.format("http://www.netfonds.no/quotes/paperhistory.php?paper=%s.OSE&csv_format=csv", ticker);
    }

    private String tickerUrl(String ticker) {
        return String.format("http://hopey.netfonds.no/derivative.php?underlying_paper=%s&underlying_exchange=OSE&type=&exchange=OMFE", ticker);
    }
    
    private String indexUrl(String ticker) {
        return String.format("http://hopey.netfonds.no/peers.php?paper=%s&exchange=OSE", ticker);
    }

    private String depthUrl(String ticker) {
        return String.format("http://www.netfonds.no/quotes/posdump.php?paper=%s.OSE&csv_format=csv", ticker);
    }

    private String purchasesUrl(String ticker) {
        return String.format("http://www.netfonds.no/quotes/tradedump.php?paper=%s.OSE&csv_format=csv", ticker);
    }
    private String accountUrl(String ticker) {

        //https://www.netfonds.no/account/order.php?paper=STL3J110.OMFE
        return String.format("https://www.netfonds.no/account/order.php?paper=%s.OMFE",ticker);
    }

    /*
    @Override
    @StoreHtmlPage
    public Page getLoginPage() {
        return loginPage;
    }

    @Override
    @StoreHtmlPage
    public Page getLogoutPage() {
        return logoutPage;
    }

    @Override
    public WebClient getWebClient() {
        return webClient;
    }

    @Override
    public void close() {
        webClient.close();
    }
    */




    public void setWebClientManager(WebClientManager webClientManager) {
        this.webClientManager = webClientManager;
    }
}
