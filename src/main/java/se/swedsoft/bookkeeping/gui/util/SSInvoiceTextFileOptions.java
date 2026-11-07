package se.swedsoft.bookkeeping.gui.util;


import org.fribok.bookkeeping.app.Path;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public final class SSInvoiceTextFileOptions {

    private SSInvoiceTextFileOptions() {
    }

    public static List<String> getInvoiceTextFileNames() {
        File iInvoiceTextDir = new File(Path.get(Path.APP_DATA), "InvoiceText");
        File[] iFiles = iInvoiceTextDir.listFiles(File::isFile);

        if (iFiles == null) {
            return Collections.emptyList();
        }

        List<String> iNames = new ArrayList<>();
        for (File iFile : iFiles) {
            iNames.add(iFile.getName());
        }

        Collator iCollator = Collator.getInstance(new Locale("sv", "SE"));
        iNames.sort(iCollator);
        return iNames;
    }
}
