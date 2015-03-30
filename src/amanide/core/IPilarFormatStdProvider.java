package amanide.core;

import amanide.natures.IPilarNature;
import amanide.utils.MisconfigurationException;

public interface IPilarFormatStdProvider {

    Object /*FormatStd*/getFormatStd();

    IPilarNature getPilarNature() throws MisconfigurationException;

    IGrammarVersionProvider getGrammarVersionProvider();

    IIndentPrefs getIndentPrefs();

}
