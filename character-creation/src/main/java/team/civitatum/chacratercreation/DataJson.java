package team.civitatum.chacratercreation;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
public final class DataJson {
    public static final Gson GSON=new GsonBuilder().serializeSpecialFloatingPointValues().setPrettyPrinting().create();
}
