GWT Object Exporter is support to share the gwt object between two widget in one page.

It export a gwt object to a
javaScriptObject . and import a javaScriptObject to a gwt object.

we know gwt JSNI can only use Element and javaScriptObject .
gwt object exporter try to export a gwt object to javaScriptObject .
so you can send this javaScriptObject to JSNI.
than get the javaScriptObject with JSNI and you can import the
javaScriptObject to gwt object.

then you can use this gwt object at all modules in one page.


Example:
moduleA{

AInterface a = new AImpl();

javaScriptObject jso = Exporter.export(a);

/
$wnd.serviceA = jso
/

}

moduleB {

javaScriptObject jso = getServiceAJso();//return $wnd.serviceA

AInterface a = Exporter.importServiceA(jso);

a.test(); }