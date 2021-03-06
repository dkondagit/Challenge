package challenge.dbside.dao.ini;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public interface MediaDao<E> {

    public void save(E entity);

    public List<E> getAll(Class<E> classType) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException;

    public void delete(E entity);

    public void update(E entity);

    public E findById(Object id, Class<E> classType) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException;
}
