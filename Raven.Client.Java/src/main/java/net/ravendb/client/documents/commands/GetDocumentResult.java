package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class GetDocumentResult {

    private ObjectNode includes;
    private ArrayNode results;
    private int nextPageStart;

    public ObjectNode getIncludes() {
        return includes;
    }

    public void setIncludes(ObjectNode includes) {
        this.includes = includes;
    }

    public ArrayNode getResults() {
        return results;
    }

    public void setResults(ArrayNode results) {
        this.results = results;
    }

    public int getNextPageStart() {
        return nextPageStart;
    }

    public void setNextPageStart(int nextPageStart) {
        this.nextPageStart = nextPageStart;
    }
}
