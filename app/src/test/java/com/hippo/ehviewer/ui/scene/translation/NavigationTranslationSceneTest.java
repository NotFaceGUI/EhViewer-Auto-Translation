package com.hippo.ehviewer.ui.scene.translation;

import org.junit.Assert;
import org.junit.Test;

public class NavigationTranslationSceneTest {
    @Test
    public void testNavCheckedItem() {
        TranslationQueueScene scene = new TranslationQueueScene();
        Assert.assertEquals(com.hippo.ehviewer.R.id.nav_translation_queue, scene.getNavCheckedItem());
    }
}
