package es.upm.fi.dia.oeg.translation;


import es.upm.fi.dia.oeg.model.RMLCMapping;
import es.upm.fi.dia.oeg.rmlc.api.model.*;
import org.apache.commons.rdf.api.IRI;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

public class RMLC2R2RML {

    private String r2rml;

    public String getR2RML(){
        return r2rml;
    }

    public void generateR2RML(RMLCMapping rmlc,String rdbName){
        r2rml ="";
        Collection<TriplesMap> triplesMaps = rmlc.getTriples();
        r2rml = getPrefix(rmlc.getContent())+"\n\n";
        triplesMaps.forEach(triplesMap -> {
            if(triplesMap.getNode().ntriplesString().matches(".*#.*"))
                r2rml += "<"+triplesMap.getNode().ntriplesString().split("#")[1]+"\n";
            else
                r2rml += "<"+triplesMap.getNode().ntriplesString().split("/")[triplesMap.getNode().ntriplesString().split("/").length-1]+"\n";
            r2rml += createLogicalTable(triplesMap.getLogicalSource());
            r2rml += createSubjectMap(triplesMap.getSubjectMap(),getClassFromMap(triplesMap.getPredicateObjectMaps()));
            r2rml += createPredicatesObjectMaps(triplesMap.getPredicateObjectMaps(),getClassFromMap(triplesMap.getPredicateObjectMaps()))+".\n\n";
        });
        try {
            BufferedWriter writer = new BufferedWriter
                    (new OutputStreamWriter(new FileOutputStream("results/"+rdbName+".r2rml.ttl"), StandardCharsets.UTF_8));
            writer.write(r2rml);
            writer.close();
        }catch (Exception e){

        }

    }

    private String getPrefix(String mappingContent){
        String prefixes = "";
        String[] lines = mappingContent.split("\\.\n");
        int i=0;
        while (lines[i].startsWith("@")){
                prefixes += lines[i]+".\n";
                i++;
        }
        prefixes +="@prefix rr: <http://www.w3.org/ns/r2rml#>.\n";
        return prefixes;
    }

    private String createLogicalTable(LogicalSource logicalSource){
       return "\trr:logicalTable [ \n\t\trr:tableName \"\\\""
                +((Source) logicalSource).getSourceName().split("/")[((Source) logicalSource).getSourceName().split("/").length-1].replace(".csv","").toUpperCase()
                +"\\\"\"; \n\t];\n";
    }

    private String createSubjectMap(SubjectMap subjectMap,String c){
        String subject="\trr:subjectMap [ \n";

        if(!subjectMap.getTemplateString().isEmpty()){
            for(int i =0;i< subjectMap.getTemplate().getColumnNames().size();i++){
                String column = subjectMap.getTemplate().getColumnName(i);
                subjectMap.getTemplate().addColumnName(i,column.toUpperCase());
            }
            subject += "\t\trr:template \"" + subjectMap.getTemplateString() +"\";\n";
        }
        if(!subjectMap.getTermType().getIRIString().isEmpty()){
            subject += "\t\trr:termType <" +subjectMap.getTermType().getIRIString() +">;\n";
        }
        for(IRI iri : subjectMap.getClasses()){
            subject += "\t\trr:class <"+ iri.getIRIString() +">;\n";
        }
        if(c!=null){
            subject += "\t\trr:class "+ c +";\n";
        }

        return subject+"\t];\n";
    }

    private String createPredicatesObjectMaps(List<PredicateObjectMap> predicateObjectMaps,String c){
        String predicates="";
        for(PredicateObjectMap predicateObjectMap : predicateObjectMaps){
            boolean flag=false;
            for(PredicateMap predicateMap : predicateObjectMap.getPredicateMaps()){
                if(!predicateMap.getConstant().ntriplesString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")) {
                    predicates +="\trr:predicateObjectMap [ \n";
                    predicates +="\t\trr:predicateMap [ rr:constant "+predicateMap.getConstant().ntriplesString()+" ];\n";
                    flag=true;
                }
            }
            if(flag) {
                for (ObjectMap objectMap : predicateObjectMap.getObjectMaps()) {
                    String o =createObjectMap(objectMap, predicateObjectMap.getPredicateMaps(), c);
                    if(!o.equals(""))
                        predicates += o+"\t];\n";
                }
                for (RefObjectMap refObjectMap : predicateObjectMap.getRefObjectMaps()) {
                    predicates += createRefObjectMap(refObjectMap, predicateObjectMap.getPredicateMaps().get(0));
                    predicates += "\t];\n";
                }

            }


        }

        return predicates;
    }

    private String createObjectMap (ObjectMap objectMap, List<PredicateMap> predicateMaps, String c){
        String reference = "";
        if(objectMap.getFunction()!=null && !objectMap.getFunction().isEmpty()){
            for(PredicateMap p: predicateMaps) {
                String column_name=p.getConstant().ntriplesString();
                if(p.getConstant().ntriplesString().matches(".*#.*")){
                    column_name =column_name.split("#")[1].replace(">", "").toUpperCase();
                }
                else{
                    column_name=column_name.split("/")[column_name.split("/").length-1].replace(">", "").toUpperCase();
                }
                reference += "\t\trr:objectMap[\n\t\t\trr:column \"" +column_name+ "\";\n\t\t];\n";
            }
        }
        if(objectMap.getDatatype()!=null && !objectMap.getDatatype().getIRIString().isEmpty()){
            reference += "\t\trr:objectMap[\n\t\t\trr:datatype <"+objectMap.getDatatype().getIRIString()+">;\n\t\t];\n";
        }
        if(objectMap.getColumn()!=null && !objectMap.getColumn().isEmpty()){
            reference += "\t\trr:objectMap[\n\t\t\trr:column \""+objectMap.getColumn().toUpperCase()+"\";\n\t\t];\n";
        }
        if(objectMap.getTemplate()!=null && !objectMap.getTemplateString().isEmpty()){
            for(int i =0;i< objectMap.getTemplate().getColumnNames().size();i++){
                String column = objectMap.getTemplate().getColumnName(i);
                objectMap.getTemplate().addColumnName(i,column.toUpperCase());
            }
            reference +="\t\trr:objectMap[\n\t\t\trr:template \""+objectMap.getTemplateString()+"\";\n\t\t];\n";
        }
        if(objectMap.getConstant()!=null && !objectMap.getConstant().ntriplesString().isEmpty()){
            if(!objectMap.getConstant().ntriplesString().equals(c))
                reference +="\t\trr:objectMap[\n\t\t\trr:constant "+objectMap.getConstant().ntriplesString()+";\n\t\t];\n";
        }
        return reference;

    }

    private String createRefObjectMap(RefObjectMap refObjectMap, PredicateMap predicateMap){
        String reference = "\t\trr:objectMap [\n";
        if(refObjectMap.getParentMap().getNode().ntriplesString().matches(".*#.*"))
            reference += "\t\t\trr:parentTriplesMap <"+refObjectMap.getParentMap().getNode().ntriplesString().split("#")[1]+";\n";
        else{
            String r = refObjectMap.getParentMap().getNode().ntriplesString().split("/")[refObjectMap.getParentMap().getNode().ntriplesString().split("/").length-1];
            reference += "\t\t\trr:parentTriplesMap <"+r+";\n";
        }

        for(Join j : refObjectMap.getJoinConditions()) {
            reference += "\t\t\trr:joinCondition [\n";
            if(j.getChild().matches(".*\\(.*")){
                if(predicateMap.getConstant().ntriplesString().matches(".*#.*"))
                    reference += "\t\t\t\t rr:child \"" + predicateMap.getConstant().ntriplesString().split("#")[1].replace(">", "").toUpperCase() + "_child\";\n";
                else {
                    String c = predicateMap.getConstant().ntriplesString().split("/")[predicateMap.getConstant().ntriplesString().split("/").length - 1];
                    reference +="\t\t\t\t rr:child \"" + c.replace(">", "").toUpperCase() + "_child\";\n";
                }
            }
            else{
                    reference+="\t\t\t\t rr:child \""+j.getChild().replace("{","").replace("}","").toUpperCase()+"\";\n";
            }
            if(j.getParent().matches(".*\\(.*")) {
                if (predicateMap.getConstant().ntriplesString().matches(".*#.*")) {
                    reference += "\t\t\t\t rr:parent \"" + predicateMap.getConstant().ntriplesString().split("#")[1].replace(">", "").toUpperCase() + "_parent\";\n";
                } else {
                    String c = predicateMap.getConstant().ntriplesString().split("/")[predicateMap.getConstant().ntriplesString().split("/").length - 1];
                    reference += "\t\t\t\t rr:parent \"" + c.replace(">", "").toUpperCase() + "_parent\";\n";
                }
            }
            else{
                reference+="\t\t\t\t rr:parent \""+j.getParent().replace("{","").replace("}","").toUpperCase()+"\";\n";
            }


            reference += "\t\t\t];\n";
        }

        return reference+"\t\t];\n";
    }


    private String getClassFromMap(List<PredicateObjectMap> pom){
        String c=null;
        for(PredicateObjectMap po: pom){
            boolean flag = false;
            for(PredicateMap p : po.getPredicateMaps()){
                if(p.getConstant().ntriplesString().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")){
                    flag=true;
                }
            }
            if(flag){
                c = po.getObjectMaps().get(0).getConstant().ntriplesString();
            }
        }
        return c;

    }


}
