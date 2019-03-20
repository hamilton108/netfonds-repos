package netfondsrepos.contenthandler;

import oahu.financial.html.EtradeBeanFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class DefaultContentHandler implements ContentHandler {
    private EtradeBeanFactory factory;

    // mandag 11/01-2016 17:59:05</span>
    private Pattern pat = Pattern.compile(".*(\\d\\d)/(\\d\\d)-(\\d\\d\\d\\d).*(\\d\\d):(\\d\\d):(\\d\\d)");

    private boolean COM_TOPMARGIN = false;
    private boolean UPDATETABLE1 = false;
    private boolean OPTION_TD = false;
    private boolean SPOT_TD = false;

    private boolean HMENUSTRIP = false;
    private boolean BOTTOMTABLE = false;
    private boolean BOTTOMTABLE_SPAN = false;
    private boolean HMENUSTRIP_SPAN = false;

    private boolean createSpotInUpdatetable = false;

    private boolean skipDerivatives = false;

    private int TD = 0;
    private static final int OPTION_NAME = 1;
    private static final int OPTION_TYPE = 2;
    private static final int X_PRICE = 3;
    private static final int EXPIRY = 4;
    private static final int BUY = 5;
    private static final int SELL = 6;
    private String optionName;
    private String optionType;
    private String xPrice;
    private String expiry;
    private String buy;
    private String sell;
    private LocalTime spotTime;
    private LocalDate spotDate;
    private int TD_SPOT = 0;
    private static final int SPOT_OPEN = 1;
    private static final int SPOT_HI = 2;
    private static final int SPOT_LO = 3;
    private static final int SPOT_CLOSE = 4;
    private static final int SPOT_VOLUME = 5;
    private String spotHi;
    private String spotLo;
    private String spotOpen;
    private String spotClose;
    private String spotVolume;

    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM-yyyy");

    public DefaultContentHandler() {
        initFields();
    }

    //region public methods
    //endregion public methods

    //region private methods
    private void initFields() {
        expiry = null;
        optionType = null;
        optionName = null;
        buy = null;
        sell = null;
    }
    private Optional<String> getAttVal(Attributes atts, String qName) {
        int index = atts.getIndex(qName);
        if (index < 0) {
            return Optional.empty();
        }
        else {
            return Optional.of(atts.getValue(index));
        }
    }

    private Optional<Boolean> attValEquals(Attributes atts, String qName, String equalsValue)  {
        Optional<String> qNameVal = getAttVal(atts,qName);
        if (qNameVal.isPresent()) {
            return Optional.of(qNameVal.get().equals(equalsValue));
        }
        else {
            return Optional.empty();
        }
    }

    private boolean attValEquals2(Attributes atts, String qName, String equalsValue)  {
        Optional<Boolean> result = attValEquals(atts,qName, equalsValue);
        if (result.isPresent()) {
            return result.get();
        }
        else {
            return false;
        }
    }
    private void printAtts(String myElement, Attributes atts) {
        System.out.println(String.format("Attributes for %s", myElement));
        for (int i=0; i<atts.getLength(); ++i) {
            System.out.println(String.format("--%s",atts.getLocalName(i)));
        }
    }
    //endregion private methods

    //region interface ContentHandler
    //region unimplemented interface methods
    @Override
    public void setDocumentLocator(Locator locator) {
    }
    @Override
    public void startDocument() throws SAXException {
    }
    @Override
    public void endDocument() throws SAXException {
    }
    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }
    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
    }
    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    }
    @Override
    public void processingInstruction(String target, String data) throws SAXException {
    }
    @Override
    public void skippedEntity(String name) throws SAXException {
    }
    //endregion unimplemented interface methods

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        if (localName.equals("table")) {
            if (attValEquals2(atts,"class", "com topmargin")) {
                COM_TOPMARGIN = true;
                //log.debug("In COM_TOPMARGIN");
            }
            else if (attValEquals2(atts,"id", "updatetable1")) {
                UPDATETABLE1 = true;
                //log.debug("In UPDATETABLE1");
            }
            else if (attValEquals2(atts,"class", "bottomtable")) {
                BOTTOMTABLE = true;
            }
            else if (attValEquals2(atts,"class", "hmenustrip")) {
                HMENUSTRIP = true;
            }
        }
        else if (COM_TOPMARGIN) {
            //log.debug("In COM_TOPMARGIN");
            if (localName.equals("td")) {
                if (!OPTION_TD) {
                    if (attValEquals2(atts,"class", "left")) {
                        //log.debug("qName==class, equalsValue==left");
                        OPTION_TD = true;
                        TD = 1;
                    }
                }
                else {
                    ++TD;
                    //log.debug(String.format("OPTION_TD: %d",TD));
                }
            }
        }
        else if (UPDATETABLE1) {
            SPOT_TD = false;
            //log.debug("UPDATETABLE1: SPOT_TD==false");
            if (localName.equals("td")) {
                //log.debug("localName.equals td");
                Optional<String> tdName = getAttVal(atts,"name");
                tdName.ifPresent(x -> {
                    switch (x) {
                        case "ju.l":
                            //log.debug("tdName == ju.l");
                            TD_SPOT = SPOT_CLOSE;
                            SPOT_TD = true;
                            break;
                        case "ju.op":
                            //log.debug("tdName == ju.op");
                            TD_SPOT = SPOT_OPEN;
                            SPOT_TD = true;
                            break;
                        case "ju.h":
                            //log.debug("tdName == ju.h");
                            TD_SPOT = SPOT_HI;
                            SPOT_TD = true;
                            break;
                        case "ju.lo":
                            //log.debug("tdName == ju.lo");
                            TD_SPOT = SPOT_LO;
                            SPOT_TD = true;
                            break;
                        case "ju.vo":
                            //log.debug("tdName == ju.vo");
                            TD_SPOT = SPOT_VOLUME;
                            SPOT_TD = true;
                            break;
                    }
                });
            }
        }
        else if (BOTTOMTABLE) {
            if (localName.equals("span")) {
                if (attValEquals2(atts,"id", "toptime")) {
                    BOTTOMTABLE_SPAN = true;
                    createSpotInUpdatetable = false;
                }
            }
        }
        else if (HMENUSTRIP) {
            if (localName.equals("span")) {
                if (attValEquals2(atts,"id", "toptime")) {
                    HMENUSTRIP_SPAN = true;
                    createSpotInUpdatetable = true;
                }
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (COM_TOPMARGIN) {
            //log.debug("COM_TOPMARGIN");
            if (localName.equals("table")) {
                //log.debug("localName == table");
                COM_TOPMARGIN = false;
            }
            else if (localName.equals("td")) {
                //log.debug("localName == td");
                if (TD == 9) {
                    OPTION_TD = false;
                    if (skipDerivatives == true) {
                        return;
                    }
                    //log.debug("TD == 9");
                    TD = 0;
                    try {
                        if (factory != null) {
                            double buyx = Double.parseDouble(buy);
                            double sellx = Double.parseDouble(sell);
                            double xPricex = Double.parseDouble(xPrice);
                            LocalDate exp = LocalDate.parse(expiry, formatter);
                            factory.onDerivativeParsed(optionName,
                                    optionType,
                                    xPricex,
                                    exp,
                                    buyx,
                                    sellx);
                        }
                    }
                    catch (NumberFormatException nex) {

                    }
                    catch (Exception ex) {
                        //System.out.println(ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }
        }
        else if (UPDATETABLE1) {
            if (localName.equals("table")) {
                UPDATETABLE1 = false;
                if (createSpotInUpdatetable) {
                    createSpot();
                }
            }
        }
        else if (HMENUSTRIP) {
            //log.debug("HMENUSTRIP");
            if (localName.equals("table")) {
                HMENUSTRIP = false;
            }
        }
        else if (BOTTOMTABLE) {
            //log.debug("BOTTOMTABLE");
            if (localName.equals("table")) {
                BOTTOMTABLE = false;
            }
        }
        else if (BOTTOMTABLE_SPAN) {
            BOTTOMTABLE_SPAN = false;
            if (!createSpotInUpdatetable) {
                createSpot();
            }
        }
        else if (HMENUSTRIP_SPAN) {
            //log.debug("HMENUSTRIP_SPAN");
            HMENUSTRIP_SPAN = false;
        }
    }

    private void createSpot() {
        //log.debug("createSpot");
        try {
            /*log.debug(
            String.format("spotOpen: %s, spotHi: %s, spotLo: %s, spotClose: %s, spotVolume: %s, spotDate: %s, spotTime: %s",
                    spotOpen,
                    spotHi,
                    spotLo,
                    spotClose,
                    spotVolume,
                    spotDate,
                    spotTime);
                    */
            if (factory != null) {
                double opn = Double.parseDouble(spotOpen);
                double hi = Double.parseDouble(spotHi);
                double lo = Double.parseDouble(spotLo);
                double close = Double.parseDouble(spotClose);
                int volume = 0;
                try {
                    volume = Integer.parseInt(spotVolume.replaceAll("[^0-9]+", ""));
                }
                catch (NumberFormatException ex) {
                }
                factory.onStockPriceParsed(spotDate,spotTime, opn, hi, lo, close, volume);
            }
        }
        catch (NumberFormatException nex) {
        }
        catch (Exception ex) {
            //System.out.println(ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (OPTION_TD) {
            String tdContent = new String(ch,start,length);
            switch (TD) {
                case OPTION_NAME: optionName = tdContent;
                    break;
                case OPTION_TYPE: optionType = tdContent;
                    break;
                case X_PRICE: xPrice = tdContent;
                    break;
                case EXPIRY: expiry = tdContent;
                    break;
                case BUY: buy = tdContent;
                    break;
                case SELL: sell = tdContent;
                    break;
            }
        }
        else if (SPOT_TD) {
            String tdContent = new String(ch,start,length);
            switch (TD_SPOT) {
                case SPOT_OPEN: spotOpen = tdContent;
                    break;
                case SPOT_HI: spotHi = tdContent;
                    break;
                case SPOT_LO: spotLo = tdContent;
                    break;
                case SPOT_CLOSE: spotClose = tdContent;
                    break;
                case SPOT_VOLUME: spotVolume = tdContent;
                    break;
            }
        }
        else if ((BOTTOMTABLE_SPAN == true) || (HMENUSTRIP_SPAN == true)){
            String tdContent = new String(ch,start,length);

            // mandag 11/01-2016 17:59:05</span>

            //(".*(\\d\\d)/(\\d\\d)-(\\d\\d\\d\\d).*(\\d\\d):(\\d\\d):(\\d\\d)")

            Matcher m = pat.matcher(tdContent);
            if (m.find()) {
                int day = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                int year = Integer.parseInt(m.group(3));
                int hours = Integer.parseInt(m.group(4));
                int minutes = Integer.parseInt(m.group(5));
                int seconds = Integer.parseInt(m.group(6));
                spotTime = LocalTime.of(hours,minutes,seconds);
                spotDate = LocalDate.of(year,month,day);
            }
            else {
            }
        }
    }


    //endregion interface ContentHandler


    //region Properties
    public void setFactory(EtradeBeanFactory factory) {
        this.factory = factory;
    }

    public void setSkipDerivatives(boolean skipDerivatives) {
        this.skipDerivatives = skipDerivatives;
    }

    //endregion Properties
}
