package net.goui.cosmicdungeon.dungeon.metrics;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public final class RunMetricsXml {
    public static final int MAX_XML_BYTES = 524288;
    private RunMetricsXml() {}
    @FunctionalInterface interface Body { void write(XMLStreamWriter writer) throws XMLStreamException; }
    static byte[] xml(Body body, int limit) throws IOException {
        try {
            var bytes = new ByteArrayOutputStream();
            var w = XMLOutputFactory.newFactory().createXMLStreamWriter(bytes, "UTF-8");
            w.writeStartDocument("UTF-8", "1.0"); body.write(w); w.writeEndDocument(); w.close();
            if (bytes.size() > limit) throw new IOException("Metrics XML exceeds " + limit + " bytes");
            return bytes.toByteArray();
        } catch (XMLStreamException | IllegalArgumentException ex) { throw new IOException("Cannot encode metrics", ex); }
    }
    static void start(XMLStreamWriter w, String name, Object... attributes) throws XMLStreamException {
        w.writeStartElement(name);
        for (int i = 0; i < attributes.length; i += 2) w.writeAttribute(attributes[i].toString(), attributes[i + 1].toString());
    }
    public static byte[] encodeRun(RunMetrics r) throws IOException {
        return xml(w -> {
            start(w, "run", "version", 1, "world", r.worldId(), "id", r.runId(), "instance", r.instanceId(),
                    "party", r.partyId(), "dungeon", r.dungeon(), "difficulty", r.difficulty(), "started", r.started(),
                    "updated", r.updated(), "ended", r.ended(), "outcome", r.outcome(), "coverage", r.coverage());
            for (var m : r.members()) {
                start(w, "member", "id", m.id(), "name", m.name(), "class", m.classId(), "status", m.status());
                stats(w, m.stats()); w.writeEndElement();
            }
            for (var e : r.resourceEvents()) {
                start(w, "resource", "transaction", e.transaction(), "donor", e.donor(), "recipient", e.recipient(),
                        "skill", e.skill(), "item", e.item(), "quantity", e.quantity(), "own", e.own(), "at", e.at());
                w.writeEndElement();
            }
            w.writeEndElement();
        }, MAX_XML_BYTES);
    }
    public static RunMetrics decodeRun(byte[] bytes) throws IOException {
        try {
            Element e = parse(bytes, MAX_XML_BYTES);
            shape(e, "run", "version world id instance party dungeon difficulty started updated ended outcome coverage", "member resource");
            version(e);
            var members = new ArrayList<RunMetrics.Member>(); var events = new ArrayList<RunMetrics.ResourceEvent>();
            for (Element child : children(e)) {
                if (child.getTagName().equals("member")) {
                    shape(child, "member", "id name class status", "stats");
                    members.add(new RunMetrics.Member(uuid(child, "id"), a(child, "name"), a(child, "class"),
                            a(child, "status"), stats(one(child, "stats"))));
                } else {
                    shape(child, "resource", "transaction donor recipient skill item quantity own at", "");
                    String own = a(child, "own");
                    if (!own.equals("true") && !own.equals("false")) throw new IOException("Invalid own flag");
                    events.add(new RunMetrics.ResourceEvent(uuid(child, "transaction"), uuid(child, "donor"),
                            uuid(child, "recipient"), a(child, "skill"), a(child, "item"), n(child, "quantity"),
                            Boolean.parseBoolean(own), n(child, "at")));
                }
            }
            var run = new RunMetrics(uuid(e, "world"), n(e, "id"), uuid(e, "party"), a(e, "dungeon"),
                    a(e, "difficulty"), n(e, "started"), n(e, "updated"), n(e, "ended"), a(e, "outcome"),
                    a(e, "coverage"), members, events);
            if (!run.instanceId().equals(a(e, "instance"))) throw new IOException("Instance identity mismatch");
            return run;
        } catch (RuntimeException ex) { throw new IOException("Invalid run metrics", ex); }
    }
    public static byte[] encodeTotals(RunMetrics.Totals t) throws IOException {
        return xml(w -> {
            start(w, "totals", "version", 1, "world", t.worldId(), "owner", t.owner(), "runs", t.runs(),
                    "completed", t.completed(), "aborted", t.aborted()); stats(w, t.stats());
            for (var h : t.recent()) {
                start(w, "recent", "instance", h.instanceId(), "id", h.runId(), "dungeon", h.dungeon(),
                        "outcome", h.outcome(), "ended", h.ended()); w.writeEndElement();
            }
            w.writeEndElement();
        }, MAX_XML_BYTES);
    }
    public static RunMetrics.Totals decodeTotals(byte[] bytes) throws IOException {
        try {
            var e = parse(bytes, MAX_XML_BYTES);
            shape(e, "totals", "version world owner runs completed aborted", "stats recent"); version(e);
            var recent = new ArrayList<RunMetrics.History>();
            for (var h : children(e)) if (h.getTagName().equals("recent")) {
                shape(h, "recent", "instance id dungeon outcome ended", "");
                recent.add(new RunMetrics.History(a(h, "instance"), n(h, "id"), a(h, "dungeon"), a(h, "outcome"), n(h, "ended")));
            }
            return new RunMetrics.Totals(uuid(e, "world"), a(e, "owner"), n(e, "runs"), n(e, "completed"),
                    n(e, "aborted"), stats(one(e, "stats")), recent);
        } catch (RuntimeException ex) { throw new IOException("Invalid metric totals", ex); }
    }
    private static void stats(XMLStreamWriter w, RunMetrics.Stats s) throws XMLStreamException {
        start(w, "stats", "kills", s.kills(), "deaths", s.deaths(), "damage", s.damage(), "healing", s.healing(),
                "resources", s.resourceEvents(), "spawners", s.spawners());
        for (var kind : List.of("mob", "used", "contributed")) {
            var map = kind.equals("mob") ? s.mobs() : kind.equals("used") ? s.used() : s.contributed();
            for (var entry : map.entrySet()) {
                start(w, kind, "id", entry.getKey(), "count", entry.getValue()); w.writeEndElement();
            }
        }
        w.writeEndElement();
    }
    private static RunMetrics.Stats stats(Element e) throws IOException {
        shape(e, "stats", "kills deaths damage healing resources spawners", "mob used contributed");
        var maps = new HashMap<String, Map<String, Long>>();
        for (String k : List.of("mob", "used", "contributed")) maps.put(k, new TreeMap<>());
        for (var child : children(e)) {
            shape(child, child.getTagName(), "id count", "");
            if (maps.get(child.getTagName()).put(a(child, "id"), n(child, "count")) != null)
                throw new IOException("Duplicate statistic");
        }
        return new RunMetrics.Stats(n(e, "kills"), n(e, "deaths"), Double.parseDouble(a(e, "damage")),
                Double.parseDouble(a(e, "healing")), n(e, "resources"), maps.get("mob"), maps.get("used"),
                maps.get("contributed"), n(e, "spawners"));
    }
    static Element parse(byte[] bytes, int limit) throws IOException {
        if (bytes.length > limit) throw new IOException("Oversized metrics XML");
        try {
            var f = DocumentBuilderFactory.newInstance();
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            f.setAttribute("http://www.oracle.com/xml/jaxp/properties/maxElementDepth", 16);
            f.setXIncludeAware(false); f.setExpandEntityReferences(false);
            var builder = f.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException e) throws SAXException { throw e; }
                public void error(SAXParseException e) throws SAXException { throw e; }
                public void fatalError(SAXParseException e) throws SAXException { throw e; }
            });
            return builder.parse(new ByteArrayInputStream(bytes)).getDocumentElement();
        } catch (Exception ex) { throw new IOException("Invalid metrics XML; original retained", ex); }
    }
    static void shape(Element e, String name, String attributes, String allowedChildren) throws IOException {
        Set<String> attrs = words(attributes), kids = words(allowedChildren);
        if (!e.getTagName().equals(name) || e.getAttributes().getLength() != attrs.size())
            throw new IOException("Unknown metrics shape: " + e.getTagName());
        for (int i = 0; i < e.getAttributes().getLength(); i++)
            if (!attrs.contains(e.getAttributes().item(i).getNodeName())) throw new IOException("Unknown metrics attribute");
        for (var child : children(e)) if (!kids.contains(child.getTagName())) throw new IOException("Unknown metrics element");
    }
    private static Set<String> words(String value) { return value.isEmpty() ? Set.of() : Set.of(value.split(" ")); }
    static List<Element> children(Element e) throws IOException {
        var result = new ArrayList<Element>();
        for (Node n = e.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element child) result.add(child);
            else if ((n.getNodeType() == Node.TEXT_NODE || n.getNodeType() == Node.CDATA_SECTION_NODE) && !n.getTextContent().isBlank())
                throw new IOException("Unexpected metrics text");
            else if (n.getNodeType() != Node.TEXT_NODE && n.getNodeType() != Node.CDATA_SECTION_NODE && n.getNodeType() != Node.COMMENT_NODE)
                throw new IOException("Unexpected metrics node");
        }
        return result;
    }
    static Element one(Element e, String name) throws IOException {
        var matches = children(e).stream().filter(c -> c.getTagName().equals(name)).toList();
        if (matches.size() != 1) throw new IOException("Expected one " + name); return matches.getFirst();
    }
    static String a(Element e, String key) throws IOException {
        if (!e.hasAttribute(key)) throw new IOException("Missing " + key); return e.getAttribute(key);
    }
    static long n(Element e, String key) throws IOException { return Long.parseLong(a(e, key)); }
    static UUID uuid(Element e, String key) throws IOException { return UUID.fromString(a(e, key)); }
    static void version(Element e) throws IOException { if (!a(e, "version").equals("1")) throw new IOException("Unsupported metrics version"); }
}
