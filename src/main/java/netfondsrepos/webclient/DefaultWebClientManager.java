package netfondsrepos.webclient;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import oahu.annotations.StoreHtmlPage;
import oahu.dto.Tuple;
import oahu.financial.html.WebClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

public class DefaultWebClientManager implements WebClientManager {
    Logger log = LoggerFactory.getLogger("netfondsrepos.webclient");
    private String netfondsProperties = "netfonds.properties";
    private boolean isRealTime = false;
    private WebClient webClient;
    private boolean javaScriptEnabled = false;
    private HtmlPage loginPage;
    private HtmlPage logoutPage;

    public DefaultWebClientManager() {
    }
    private String orderPageUrl(String ticker) {
        return String.format(
                "https://www.netfonds.no/account/order.php?paper=%S.OMFE", ticker);
    }


    //region Interface WebClientManager
    @Override
    public Page getPage(String url) throws IOException {
        WebClient client = getWebClient();
        return client.getPage(url);
    }

    private Page getOrderPage(String optionTicker) throws IOException {
        return null;
    }
    //endregion

    //region Local Methods
    private WebClient getWebClient() throws IOException {
        if (webClient == null) {
            webClient = new WebClient();
            webClient.getOptions().setJavaScriptEnabled(javaScriptEnabled);
        }
        if (isRealTime) {
            loginPage = login();
        }
        return webClient;
    }

    @StoreHtmlPage(desc = "DefaultWebClientManager.login")
    private HtmlPage login() throws IOException {
        Tuple<String> prm = logonParam();

        HtmlPage page = webClient.getPage("https://bang.netfonds.no/auth.php");
        HtmlForm form = page.getFormByName("login");

        HtmlSubmitInput button = form.getInputByName("confirm");
        HtmlTextInput customer = form.getInputByName("customer");
        HtmlPasswordInput password = form.getInputByName("password");

        customer.setValueAttribute(prm.first());
        password.setValueAttribute(prm.second());

        return button.click();
    }

    @Override
    public Optional<Page> logout() throws IOException {
        log.info("[DefaultWebClientManager] logout()");
        if (loginPage == null) {
            return Optional.empty();
        }
        String logoutAnchor = "https://www.netfonds.no/account/logout.php";
        HtmlAnchor logout = loginPage.getAnchorByHref(logoutAnchor);
        logoutPage = logout.click();
        return Optional.of(logoutPage);
    }

    private Tuple<String> logonParam() throws IOException {
        Properties prop = new Properties();
        //InputStream input = null;

        //input = new FileInputStream(netfondsProperties);
        //prop.load(input);
        prop.load(DefaultWebClientManager.class.getResourceAsStream(String.format("/%s",netfondsProperties)));
        return new Tuple<>(
                    prop.getProperty("netfonds.user"),
                    prop.getProperty("netfonds.password"));
    }
    //endregion

    //region Properties
    public void setNetfondsProperties(String netfondsProperties) {
        this.netfondsProperties = netfondsProperties;
    }

    public void setRealTime(boolean realTime) {
        isRealTime = realTime;
    }

    public void setJavaScriptEnabled(boolean javaScriptEnabled) {
        this.javaScriptEnabled = javaScriptEnabled;
    }
    //endregion
}
