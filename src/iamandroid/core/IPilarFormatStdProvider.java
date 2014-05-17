package iamandroid.core;

import iamandroid.natures.IPilarNature;
import iamandroid.utils.MisconfigurationException;

public interface IPilarFormatStdProvider {

    Object /*FormatStd*/getFormatStd();

    IPilarNature getPilarNature() throws MisconfigurationException;

    IGrammarVersionProvider getGrammarVersionProvider();

    IIndentPrefs getIndentPrefs();

}
