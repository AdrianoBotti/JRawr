// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Elemental/RotationOptions.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.elemental;

public class RotationOptions implements IRotationOptions {
    public RotationOptions(boolean useFn, boolean useCl, boolean useDpsFireTotem, boolean useFireEle) {
        useFireNova = useFn;
        useChainLightning = useCl;
        this.useDpsFireTotem = useDpsFireTotem;
        this.useFireEle = useFireEle;
    }

    private boolean useFireNova;

    @Override
    public boolean getUseFireNova() {
        return useFireNova;
    }

    public void setUseFireNova(boolean value) {
        useFireNova = value;
    }

    private boolean useChainLightning;

    @Override
    public boolean getUseChainLightning() {
        return useChainLightning;
    }

    public void setUseChainLightning(boolean value) {
        useChainLightning = value;
    }

    private boolean useDpsFireTotem;

    @Override
    public boolean getUseDpsFireTotem() {
        return useDpsFireTotem;
    }

    public void setUseDpsFireTotem(boolean value) {
        useDpsFireTotem = value;
    }

    private boolean useFireEle;

    @Override
    public boolean getUseFireEle() {
        return useFireEle;
    }

    public void setUseFireEle(boolean value) {
        useFireEle = value;
    }
}
