package uk.gov.moj.cpp.subscriptions.event.processor.filterprocessing.events;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.List;

public class Section {
    private List<String> lines = new ArrayList<>();
    public Section(final String... lines){
       this.lines.addAll(stream(lines).collect(toList()));
    }

    public static Section buildSection(final String... lines){
        return new Section(lines);
    }

    public List<String> getLines() {
        return lines;
    }
}
