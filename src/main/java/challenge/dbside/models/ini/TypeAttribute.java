package challenge.dbside.models.ini;


//TODO: name TypeAttribute and TypeOfAttribute  ???
public enum TypeAttribute {
	STRING(1),
	INT(2),
	DATE(3),
	REF_ONE_DIRECTIONAL(4),
	REF_TWO_DIRECTIONAL(5);
	
	private final int value;

    private TypeAttribute(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
