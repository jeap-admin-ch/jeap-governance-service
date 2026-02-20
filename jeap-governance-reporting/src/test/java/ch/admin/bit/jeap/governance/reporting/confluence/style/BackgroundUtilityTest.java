package ch.admin.bit.jeap.governance.reporting.confluence.style;

import ch.admin.bit.jeap.governance.reporting.confluence.model.State;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BackgroundUtilityTest {

    @Test
    void getBackgroundColorScore(){
        assertEquals("background-color: #e3fcef;", BackgroundUtility.getBackgroundColor(80));
        assertEquals("background-color: #e3fcef;", BackgroundUtility.getBackgroundColor(100));
        assertEquals("background-color: #fff7e6;", BackgroundUtility.getBackgroundColor(50));
        assertEquals("background-color: #fff7e6;", BackgroundUtility.getBackgroundColor(79));
        assertEquals("background-color: #ffe9e9;", BackgroundUtility.getBackgroundColor(0));
        assertEquals("background-color: #ffe9e9;", BackgroundUtility.getBackgroundColor(49));
        assertEquals("background-color: #f0f0f0;", BackgroundUtility.getBackgroundColor(-1));
    }

     @Test
    void getBackgroundColorState(){
        assertEquals("background-color: #e3fcef;", BackgroundUtility.getBackgroundColor(State.OK));
        assertEquals("background-color: #ffe9e9;", BackgroundUtility.getBackgroundColor(State.FAIL));
        assertEquals("background-color: #f0f0f0;", BackgroundUtility.getBackgroundColor(State.UNKNOWN));
        assertEquals("background-color: #f0f0f0;", BackgroundUtility.getBackgroundColor(State.PAUSED));
        assertEquals("background-color: #f0f0f0;", BackgroundUtility.getBackgroundColor(State.DISABLED));
    }

}
