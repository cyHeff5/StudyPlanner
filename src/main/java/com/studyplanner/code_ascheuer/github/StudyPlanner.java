package com.studyplanner.code_ascheuer.github;

import DataAccess.EventUpdateDB;
import DataAccess.EventsDeleteDB;
import DataAccess.LoadEventDB;
import DataAccess.LoadModulDDB;
import Model.Event;
import Model.Modul;
import View.ButtonAndElement;
import View.EditandDeleteModul;
import View.Helper;
import com.calendarfx.model.Calendar;
import com.calendarfx.model.Calendar.Style;
import com.calendarfx.model.CalendarEvent;
import com.calendarfx.model.Entry;
import com.calendarfx.view.CalendarView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static Helper.LocalDateTimeConverter.convertEventToEntry;


/**
 * The type Study planner.
 */
public class StudyPlanner extends Application {

    public final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("StudyPlanner");
    public final EntityManager entityManager = entityManagerFactory.createEntityManager();
    public final EntityTransaction entityTransaction = entityManager.getTransaction();
    /**
     * The Module.
     */
    final List<Modul> Module = new ArrayList<>();
    /**
     * The Events.
     */
    final List<Event> Events = new ArrayList<>();
    /**
     * The School time table.
     */
    final Calendar SchoolTimeTable = new Calendar("Stundenplan");
    /**
     * The Study plan.
     */
    final Calendar StudyPlan = new Calendar("Lernplan");
    final ButtonAndElement buttonAndElement = new ButtonAndElement();
    final Helper helper = new Helper();
    /**
     * The Listbox.
     */
    final ListView<Button> listbox = new ListView<>();

    /**
     * The entry point of application.
     *
     * @param args
     *         the input arguments
     */
    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage stage) {

        
        CalendarView calendarView = new CalendarView();

        calendarEventHandler();

        SchoolTimeTable.setStyle(Style.STYLE2);
        StudyPlan.setStyle(Style.STYLE3);

        CompletableFuture.runAsync(() -> {

        /*
        @Marc Load Data from DB and add Module to listbox
         */
            LoadModulDDB loadModulDDB = new LoadModulDDB();
            for (Modul modul : loadModulDDB.zeigemodul(entityManager, entityTransaction)) {
                Module.add(modul);
                modul.setEcts(modul.gettEcts());
                Button bt = new Button(modul.toString2());
                listbox.getItems().add(bt);
                EditandDeleteModul editandDeleteModul = new EditandDeleteModul();
                bt.setOnAction(actionEvent -> editandDeleteModul.editModul(modul, bt, entityManager, entityTransaction, Module, listbox, Events, SchoolTimeTable, StudyPlan));

            }
            LoadEventDB loadEventDB = new LoadEventDB();
            for (Event event : loadEventDB.zeigeEvent(entityManager, entityTransaction)) {
                Events.add(event);
                Entry<?> entry = convertEventToEntry(event);
                if (Objects.equals(event.getCalendar(), "Stundenplan")) {
                    SchoolTimeTable.addEntry(entry);
                } else if (Objects.equals(event.getCalendar(), "Lernplan")) {
                    StudyPlan.addEntry(entry);
                }

            }
            
        });


        helper.initializingCalenderView(calendarView, StudyPlan, SchoolTimeTable);

        Button BtCreateEvent = buttonAndElement.getBtCreateEvent(Module, Events, StudyPlan, SchoolTimeTable, entityManager, entityTransaction);
        Button BtCreateFiller = buttonAndElement.getBtCreateFillerEvent(Module, Events, StudyPlan, entityManager, entityTransaction);
        Button BtCreateModul = buttonAndElement.getBtCreateModul(Module, listbox, entityManager, entityTransaction, Events, SchoolTimeTable, StudyPlan);
        Button BtDeleteModul = buttonAndElement.getBtDeleteModul(Module, Events, SchoolTimeTable, StudyPlan, entityManager, entityTransaction, listbox);
        Button BtGenerateSP = buttonAndElement.getBtGenerateStudyPlan(Module, Events, StudyPlan, entityManager, entityTransaction, listbox);
        Button BtShowQuote = buttonAndElement.getBtShowQuote();
        Pane leftSideSplitPane = buttonAndElement.getLeftSideSplitPane(BtCreateEvent, BtCreateFiller, BtCreateModul, BtDeleteModul, BtGenerateSP, listbox, BtShowQuote);

        BtCreateEvent.setMinWidth(200);
        BtCreateFiller.setMinWidth(200);
        BtCreateModul.setMinWidth(200);
        BtDeleteModul.setMinWidth(200);
        BtGenerateSP.setMinWidth(200);
        BtShowQuote.setMinWidth(200);

        listbox.setMaxWidth(200);

        SplitPane split = new SplitPane(leftSideSplitPane, calendarView);
        leftSideSplitPane.setMaxWidth(200);
        leftSideSplitPane.setMinWidth(200);
        Scene sceneO = new Scene(split);
        stage.setScene(sceneO);
        stage.setMinWidth(1000);
        stage.setHeight(780);
        stage.centerOnScreen();
        stage.setTitle("Study Planer");
        stage.show();
        
    }

    /**
     * Calendar event handler.
     * <p>
     * Adding the EventHandler to the Calendar´s
     *
     * @author Andreas Scheuer
     */
    public void calendarEventHandler() {
        Platform.runLater(() -> {
            StudyPlan.addEventHandler(event -> checkEvent(event));

            SchoolTimeTable.addEventHandler(event -> checkEvent(event));
        });
    }
    
    private Event findEventId(String id) {
        for( Event event : Events) {
            if(event.getId().equals(id)) {
                return event;
            }
        }
        return null;
    }

    
    public void checkEvent(CalendarEvent event) {
        Event eventId = findEventId(event.getEntry().getId());

        if(event.isEntryAdded()) {
            isEntryAdded(event);
        }

        if(event.isEntryRemoved()) {
            isEntryRemoved(event);
        }

        if(event.getCalendar() != null && event.getOldCalendar() != null && !event.getCalendar().equals(event.getOldCalendar())) {
            isEntryCalendarChanged(event);
        }

        if(event.getOldText() != null && !event.getOldText().equals(event.getEntry().getTitle())) {
            isEntryTitleChanged(event);
        }

        if(event.getOldInterval() != null && (!event.getOldInterval().getDuration().equals(event.getEntry().getInterval().getDuration()) || event.isDayChange())) {
            System.out.println(event.getOldInterval());
            isEntryIntervallChanged(event);
        }

        if(eventId != null && !eventId.getStartTime().equals(event.getEntry().getStartTime().toString())) {
            isEntryIntervallChanged2(event);
        }
    }

    public void isEntryIntervallChanged(CalendarEvent event) {
        System.out.println("Intervall changed");
        EventUpdateDB eventUpdateDB = new EventUpdateDB();
        Events.stream().filter(e -> e.getId().equals(event.getEntry().getId())).forEach(e -> {
            e.setStartTime(event.getEntry().getStartTime().toString());
            e.setStarDate(event.getEntry().getStartDate().toString());
            e.setEndTime(event.getEntry().getEndTime().toString());
            e.setEndDate(event.getEntry().getEndDate().toString());
            eventUpdateDB.updateEvent(e, entityManager, entityTransaction);
        });


        Module.stream().filter(e -> e.getUuid().contains(event.getEntry().getId())).forEach(e -> {
                    e.getEcts().setDuration(e.getEcts().getDuration().plus(event.getOldInterval().getDuration().minus(event.getEntry().getDuration())));

                    helper.changeListBoxButtonText(e, listbox);
                }

        );
    }
    
    public void isEntryIntervallChanged2(CalendarEvent event) {
        EventUpdateDB eventUpdateDB = new EventUpdateDB();
        Events.stream().filter(e -> e.getId().equals(event.getEntry().getId())).forEach(e -> {
            e.setStartTime(event.getEntry().getStartTime().toString());
            e.setEndTime(event.getEntry().getEndTime().toString());
            e.setStarDate(event.getEntry().getStartDate().toString());
            e.setEndDate(event.getEntry().getEndDate().toString());
            eventUpdateDB.updateEvent(e, entityManager, entityTransaction);
        });
    }
    
    public void isEntryTitleChanged(CalendarEvent event) {
        System.out.println("Title changed");
        EventUpdateDB eventUpdateDB = new EventUpdateDB();
        Events.stream().filter(e -> e.getId().equals(event.getEntry().getId())).forEach(e -> {
            e.setTitle(event.getEntry().getTitle());
            eventUpdateDB.updateEvent(e, entityManager, entityTransaction);
        });
    }
    
    public void isEntryCalendarChanged(CalendarEvent event) {
        EventUpdateDB eventUpdateDB = new EventUpdateDB();
        Events.stream().filter(e -> e.getId().equals(event.getEntry().getId())).forEach(e -> {
            e.setCalendar(event.getCalendar().getName());
            eventUpdateDB.updateEvent(e, entityManager, entityTransaction);
        });
    }
    
    public void isEntryRemoved(CalendarEvent event) {
        for (Modul modul : Module) {
            if (modul.getUuid().contains(event.getEntry().getId())) {
                modul.getEcts().setDuration(modul.getEcts().getDuration().plus(event.getEntry().getDuration()));

                helper.changeListBoxButtonText(modul, listbox);

                /*
                removes the Events from the Eventlist
                */
                List<Event> events = Events.stream().filter(e -> e.getId().equals(event.getEntry().getId())).collect(Collectors.toList());
                EventsDeleteDB eventsDeleteDB = new EventsDeleteDB();
                eventsDeleteDB.EventDelete(events, entityManager, entityTransaction);
                Events.removeAll(events);

            }
        }
        if(event.getEntry().getTitle().equals("Filler")) {
            List<Event> events = Events.stream().filter((e -> e.getId().equals(event.getEntry().getId()))).collect(Collectors.toList());
            EventsDeleteDB eventsDeleteDB = new EventsDeleteDB();
            eventsDeleteDB.EventDelete(events, entityManager, entityTransaction);
            Events.removeAll(events);
        }
    }
    
    /**
     * Is entry added.
     *
     * @param event
     *         the event
     */
    public void isEntryAdded(CalendarEvent event) {
        for (Modul modul : Module) {
            if (modul.getUuid().contains(event.getEntry().getId())) {
                modul.getEcts().setDuration(modul.getEcts().getDuration().minus(event.getEntry().getDuration()));

                helper.changeListBoxButtonText(modul, listbox);
            }
        }
    }
}
