package com.example.notes;

import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.*;
import javax.microedition.rms.*;
import java.util.Vector;

/**
 * Minimal iOS-inspired notes app for MIDP 2.0 / CLDC 1.1.
 * Designed for small feature-phone screens and keypad navigation.
 */
public final class NotesMIDlet extends MIDlet implements CommandListener {
    private Display display;
    private List noteList;
    private Form editor;
    private TextField titleField;
    private TextField bodyField;
    private Command openCommand = new Command("Open", Command.ITEM, 1);
    private Command addCommand = new Command("New", Command.SCREEN, 1);
    private Command editCommand = new Command("Edit", Command.SCREEN, 2);
    private Command deleteCommand = new Command("Delete", Command.SCREEN, 3);
    private Command saveCommand = new Command("Save", Command.OK, 1);
    private Command backCommand = new Command("Back", Command.BACK, 2);
    private Command cancelCommand = new Command("Cancel", Command.CANCEL, 3);
    private RecordStore store;
    private Vector notes = new Vector();
    private int editingId = -1;

    private static final class Note {
        int id;
        String title;
        String body;
        Note(int i, String t, String b) { id=i; title=t; body=b; }
    }

    public void startApp() {
        display = Display.getDisplay(this);
        try {
            store = RecordStore.openRecordStore("ios-notes", true);
            loadNotes();
        } catch (Exception e) {
            showError("Storage error: " + e.toString());
        }
        showList();
    }

    public void pauseApp() { }

    public void destroyApp(boolean unconditional) {
        try { if (store != null) store.closeRecordStore(); } catch (Exception e) { }
    }

    private void loadNotes() throws Exception {
        notes.removeAllElements();
        if (store == null) return;
        RecordEnumeration en = store.enumerateRecords(null, null, false);
        while (en.hasNextElement()) {
            int id = en.nextRecordId();
            byte[] raw = store.getRecord(id);
            String packed = new String(raw, "UTF-8");
            int split = packed.indexOf('\n');
            String title = split < 0 ? packed : packed.substring(0, split);
            String body = split < 0 ? "" : packed.substring(split + 1);
            notes.addElement(new Note(id, title, body));
        }
        en.destroy();
    }

    private void showList() {
        noteList = new List("Notes", List.IMPLICIT);
        noteList.addCommand(addCommand);
        noteList.addCommand(editCommand);
        noteList.addCommand(deleteCommand);
        noteList.setCommandListener(this);
        if (notes.size() == 0) {
            noteList.append("No notes yet", null);
        } else {
            for (int i=0; i<notes.size(); i++) {
                Note n = (Note)notes.elementAt(i);
                noteList.append(n.title.length()==0 ? "Untitled" : n.title, null);
            }
        }
        display.setCurrent(noteList);
    }

    private void openEditor(Note note) {
        editingId = note == null ? -1 : note.id;
        editor = new Form(note == null ? "New Note" : "Edit Note");
        titleField = new TextField("Title", note == null ? "" : note.title, 60, TextField.ANY);
        bodyField = new TextField("Note", note == null ? "" : note.body, 3000, TextField.ANY);
        editor.append(titleField);
        editor.append(bodyField);
        editor.addCommand(saveCommand);
        editor.addCommand(cancelCommand);
        editor.setCommandListener(this);
        display.setCurrent(editor);
    }

    private void saveNote() {
        try {
            String title = titleField.getString().trim();
            String body = bodyField.getString();
            byte[] data = (title + "\n" + body).getBytes("UTF-8");
            if (editingId < 0) store.addRecord(data, 0, data.length);
            else store.setRecord(editingId, data, 0, data.length);
            loadNotes();
            showList();
        } catch (Exception e) {
            showError("Could not save: " + e.toString());
        }
    }

    private void deleteSelected() {
        int index = noteList.getSelectedIndex();
        if (index < 0 || index >= notes.size()) return;
        Note n = (Note)notes.elementAt(index);
        try {
            store.deleteRecord(n.id);
            loadNotes();
            showList();
        } catch (Exception e) { showError("Could not delete: " + e.toString()); }
    }

    private void showError(String message) {
        Alert a = new Alert("Notes", message, null, AlertType.ERROR);
        a.setTimeout(Alert.FOREVER);
        display.setCurrent(a);
    }

    public void commandAction(Command c, Displayable d) {
        if (d == noteList) {
            if (c == addCommand) openEditor(null);
            else if (c == editCommand || c == List.SELECT_COMMAND || c == openCommand) {
                int i = noteList.getSelectedIndex();
                if (i >= 0 && i < notes.size()) openEditor((Note)notes.elementAt(i));
            } else if (c == deleteCommand) deleteSelected();
        } else if (d == editor) {
            if (c == saveCommand) saveNote();
            else if (c == cancelCommand) showList();
        }
    }
                            }
