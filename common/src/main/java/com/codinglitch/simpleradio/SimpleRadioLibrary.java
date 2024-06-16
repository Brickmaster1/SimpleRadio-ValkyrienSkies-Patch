package com.codinglitch.simpleradio;

import com.codinglitch.lexiconfig.LexiconfigApi;
import com.codinglitch.lexiconfig.Library;
import com.codinglitch.lexiconfig.annotations.LexiconLibrary;
import com.codinglitch.simpleradio.core.central.Frequency;
import com.codinglitch.simpleradio.core.registry.SimpleRadioItems;

@LexiconLibrary
public class SimpleRadioLibrary extends Library {
    public static SimpleRadioServerConfig SERVER_CONFIG = new SimpleRadioServerConfig();

    @Override
    public void shelveLexicons() {
        LexiconfigApi.shelveLexicon(SERVER_CONFIG);

        LexiconfigApi.registerListener(LexiconfigApi.EventType.POST_REVISION, (event) -> Frequency.onLexiconRevision());
        LexiconfigApi.registerListener(LexiconfigApi.EventType.POST_CATALOG, (event) -> Frequency.onLexiconRevision());

        LexiconfigApi.registerListener(LexiconfigApi.EventType.POST_REVISION, (event) -> CompatCore.reloadCompatibilities());
        LexiconfigApi.registerListener(LexiconfigApi.EventType.POST_CATALOG, (event) -> CompatCore.spoutCompatibilities());

        LexiconfigApi.registerListener(LexiconfigApi.EventType.POST_REVISION, (event) -> SimpleRadioItems.reload());
        LexiconfigApi.registerListener(LexiconfigApi.EventType.POST_CATALOG, (event) -> SimpleRadioItems.reload());
    }
}
