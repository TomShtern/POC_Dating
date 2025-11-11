package com.dating.ui.views;

import com.dating.ui.dto.Match;
import com.dating.ui.service.MatchService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Matches view - Display all matches
 */
@Route(value = "matches", layout = MainLayout.class)
@PageTitle("My Matches | POC Dating")
@PermitAll
@Slf4j
public class MatchesView extends VerticalLayout {

    private final MatchService matchService;
    private Grid<Match> matchGrid;

    public MatchesView(MatchService matchService) {
        this.matchService = matchService;

        setSizeFull();
        setPadding(true);

        createUI();
        loadMatches();
    }

    private void createUI() {
        H2 title = new H2("Your Matches");

        matchGrid = new Grid<>(Match.class, false);
        matchGrid.addColumn(match -> match.getOtherUser().getFirstName()).setHeader("Name");
        matchGrid.addColumn(match -> match.getOtherUser().getAge()).setHeader("Age");
        matchGrid.addColumn(match -> match.getOtherUser().getCity()).setHeader("Location");
        matchGrid.addColumn(Match::getCreatedAt).setHeader("Matched On");

        matchGrid.addItemClickListener(event -> {
            Match match = event.getItem();
            UI.getCurrent().navigate(MessagesView.class);
        });

        matchGrid.setSizeFull();

        add(title, matchGrid);
    }

    private void loadMatches() {
        try {
            List<Match> matches = matchService.getMyMatches();
            matchGrid.setItems(matches);

            if (matches.isEmpty()) {
                add(new Paragraph("No matches yet. Keep swiping!"));
            }

        } catch (Exception ex) {
            log.error("Failed to load matches", ex);
            add(new Paragraph("Failed to load matches"));
        }
    }
}
