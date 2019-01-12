import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class HTTPReader{
    private HashMap<String, String> httpHeader;

    public HTTPReader(){
        httpHeader = new HashMap<>();
    }

    public void add (String key, String line){
        httpHeader.put(key, line);
    }

    public boolean contains (String key){
        if(httpHeader.containsKey(key)){
            return true;
        }
        return false;
    }

    public String getValue(String key){
        return httpHeader.get(key);
    }

    public void print(){
        httpHeader.forEach((k,v) ->{
            System.out.println(k + v);
        });
    }

    public void reset(){
        httpHeader = new HashMap<String, String>();
    }
}