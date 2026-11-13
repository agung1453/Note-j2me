package com.example.notes;

import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.*;
import javax.microedition.rms.*;
import java.util.Vector;
import java.util.Date;

/**
 * Notes app with a custom iOS Notes-inspired list screen.
 * MIDP 2.0 / CLDC 1.1; designed for Nokia Series 40 keypad phones.
 */
public final class NotesMIDlet extends MIDlet implements CommandListener {
    private Display display;
    private NotesCanvas home;
    private Form editor;
    private TextField titleField;
    private TextField bodyField;
    private Command newCommand = new Command("New", Command.SCREEN, 1);
    private Command openCommand = new Command("Open", Command.ITEM, 1);
    private Command deleteCommand = new Command("Delete", Command.SCREEN, 2);
    private Command saveCommand = new Command("Save", Command.OK, 1);
    private Command cancelCommand = new Command("Cancel", Command.CANCEL, 2);
    private Command exitCommand = new Command("Exit", Command.EXIT, 3);
    private RecordStore store;
    private Vector notes = new Vector();
    private int editingId = -1;
    private int selected = 0;

    private static final class Note {
        int id;
        String title;
        String body;
        long updated;
        Note(int i, String t, String b, long u) {
            id=i; title=t; body=b; updated=u;
        }
    }

    public void startApp() {
        display = Display.getDisplay(this);
        try {
            store = RecordStore.openRecordStore("ios-notes", true);
            loadNotes();
        } catch (Exception e) {
            showError("Storage error");
        }
        showHome();
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
            int first = packed.indexOf('\n');
            int second = first < 0 ? -1 : packed.indexOf('\n', first + 1);
            String title = first < 0 ? packed : packed.substring(0, first);
            String body = second < 0 ? (first < 0 ? "" : packed.substring(first + 1))
                                     : packed.substring(first + 1, second);
            long updated = System.currentTimeMillis();
            if (second >= 0) {
                try { updated = Long.parseLong(packed.substring(second + 1)); }
                catch (Exception ignored) { }
            }
            notes.addElement(new Note(id, title, body, updated));
        }
        en.destroy();
        if (selected >= notes.size()) selected = notes.size() - 1;
        if (selected < 0) selected = 0;
    }

    private void showHome() {
        home = new NotesCanvas();
        home.addCommand(newCommand);
        home.addCommand(openCommand);
        home.addCommand(deleteCommand);
        home.addCommand(exitCommand);
        home.setCommandListener(this);
        display.setCurrent(home);
    }

    private void openEditor(Note note) {
        editingId = note == null ? -1 : note.id;
        editor = new Form(note == null ? "New Note" : "Edit Note");
        titleField = new TextField("Title", note == null ? "" : note.title, 80, TextField.ANY);
        bodyField = new TextField("Note", note == null ? "" : note.body, 3000,
                                  TextField.ANY | TextField.NON_PREDICTIVE);
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
            long now = System.currentTimeMillis();
            byte[] data = (title + "\n" + body + "\n" + now).getBytes("UTF-8");
            if (editingId < 0) store.addRecord(data, 0, data.length);
            else store.setRecord(editingId, data, 0, data.length);
            loadNotes();
            showHome();
        } catch (Exception e) {
            showError("Could not save note");
        }
    }

    private void deleteSelected() {
        if (selected < 0 || selected >= notes.size()) return;
        try {
            Note n = (Note)notes.elementAt(selected);
            store.deleteRecord(n.id);
            loadNotes();
            showHome();
        } catch (Exception e) { showError("Could not delete note"); }
    }

    private void showError(String message) {
        Alert a = new Alert("Notes", message, null, AlertType.ERROR);
        a.setTimeout(Alert.FOREVER);
        display.setCurrent(a);
    }

    private String dateLabel(long time) {
        Date d = new Date(time);
        return two(d.getDate()) + "/" + two(d.getMonth()+1) + "/" + (1900+d.getYear());
    }

    private String two(int n) { return n < 10 ? "0"+n : ""+n; }

    public void commandAction(Command c, Displayable d) {
        if (d == home) {
            if (c == newCommand) openEditor(null);
            else if (c == openCommand) {
                if (selected >= 0 && selected < notes.size())
                    openEditor((Note)notes.elementAt(selected));
            } else if (c == deleteCommand) deleteSelected();
            else if (c == exitCommand) {
                destroyApp(false);
                notifyDestroyed();
            }
        } else if (d == editor) {
            if (c == saveCommand) saveNote();
            else if (c == cancelCommand) showHome();
        }
    }

    private final class NotesCanvas extends Canvas {
        private Font normal = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        private Font bold = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);
        private Font large = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
        private int scroll = 0;

        NotesCanvas() {
            setFullScreenMode(false);
        }

        protected void paint(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            g.setColor(250, 249, 245);
            g.fillRect(0, 0, w, h);

            // Warm Notes-style header.
            g.setColor(255, 216, 77);
            g.fillRect(0, 0, w, 48);
            g.setColor(65, 53, 20);
            g.setFont(large);
            g.drawString("Notes", 10, 8, Graphics.TOP | Graphics.LEFT);
            g.setFont(normal);
            g.drawString(notes.size() + (notes.size()==1 ? " note" : " notes"),
                         w-10, 18, Graphics.TOP | Graphics.RIGHT);

            int top = 54;
            int bottom = h - 22;
            int rowH = 54;
            int visible = (bottom - top) / rowH;
            if (selected < scroll) scroll = selected;
            if (selected >= scroll + visible && visible > 0) scroll = selected-visible+1;

            if (notes.size() == 0) {
                g.setColor(130, 126, 116);
                g.setFont(bold);
                g.drawString("No Notes", w/2, top+38, Graphics.TOP | Graphics.HCENTER);
                g.setFont(normal);
                g.drawString("Press New to create one", w/2, top+58,
                             Graphics.TOP | Graphics.HCENTER);
            } else {
                for (int i=scroll; i<notes.size() && i<scroll+visible+1; i++) {
                    int y = top + (i-scroll)*rowH;
                    Note n = (Note)notes.elementAt(i);
                    if (i == selected) {
                        g.setColor(255, 239, 184);
                        g.fillRoundRect(5, y+1, w-10, rowH-3, 8, 8);
                    }
                    g.setColor(45, 43, 38);
                    g.setFont(bold);
                    String title = n.title.length()==0 ? "Untitled" : n.title;
                    g.drawString(shorten(title, w-28, bold), 13, y+5,
                                 Graphics.TOP | Graphics.LEFT);
                    g.setColor(105, 101, 92);
                    g.setFont(normal);
                    String preview = n.body.replace('\n', ' ');
                    if (preview.length()==0) preview = "No additional text";
                    g.drawString(shorten(preview, w-28, normal), 13, y+22,
                                 Graphics.TOP | Graphics.LEFT);
                    g.setColor(145, 139, 126);
                    g.drawString(dateLabel(n.updated), 13, y+38,
                                 Graphics.TOP | Graphics.LEFT);
                    g.setColor(225, 221, 211);
                    g.drawLine(12, y+rowH-1, w-12, y+rowH-1);
                }
            }

            // Softkey hint strip; phone still handles actual command menu.
            g.setColor(238, 235, 226);
            g.fillRect(0, h-20, w, 20);
            g.setColor(70, 65, 54);
            g.setFont(normal);
            g.drawString("Open", 8, h-17, Graphics.TOP | Graphics.LEFT);
            g.drawString("New", w/2, h-17, Graphics.TOP | Graphics.HCENTER);
            g.drawString("Options", w-8, h-17, Graphics.TOP | Graphics.RIGHT);
        }

        private String shorten(String s, int maxWidth, Font f) {
            if (f.stringWidth(s) <= maxWidth) return s;
            String dots = "...";
            while (s.length() > 0 && f.stringWidth(s+dots) > maxWidth)
                s = s.substring(0, s.length()-1);
            return s + dots;
        }

        protected void keyPressed(int keyCode) {
            int action = getGameAction(keyCode);
            if (action == UP) {
                if (selected > 0) selected--;
                repaint();
            } else if (action == DOWN) {
                if (selected < notes.size()-1) selected++;
                repaint();
            } else if (action == FIRE) {
                if (selected >= 0 && selected < notes.size())
                    openEditor((Note)notes.elementAt(selected));
            }
        }
    }
}
