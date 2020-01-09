package com.epgpbot.database;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class AbstractCursor implements Cursor {
  protected abstract void setFailed(Exception e);

  @Override
  public Stream<Cursor> stream() {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), 0), false);
  }

  @Override
  public <T> Stream<T> stream(Supplier<T> supplier) {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterate(supplier).iterator(), 0), false);
  }

  @Override
  public <T> T scan(T object) throws Exception {
    return scan(() -> object);
  }

  @Override
  public <T> Iterable<T> iterate(T object) {
    return iterate(() -> object);
  }

  @Override
  public <T> Stream<T> stream(T object) {
    return stream(() -> object);
  }

  @Override
  public <T> List<T> toList(Supplier<T> supplier) throws Exception {
    return stream(supplier).collect(Collectors.toCollection(ArrayList::new));
  }

  @Override
  public <T> T scan(Supplier<T> supplier) throws Exception {
    try {
      T object = supplier.get();

      for (Field f : object.getClass().getDeclaredFields()) {
        if (!f.canAccess(object)) {
          continue;
        }
        if (f.getAnnotation(DBIgnore.class) != null) {
          continue;
        }

        String name = f.getName();
        DBField nameOverride = f.getAnnotation(DBField.class);
        if (nameOverride != null) {
          name = nameOverride.value();
        }

        if (f.getType().isEnum()) {
          f.set(object, f.getType().getEnumConstants()[get(name, Integer.class)]);
        }
        else {
          f.set(object, get(name, f.getType()));
        }
      }

      return object;
    } catch (Exception e) {
      setFailed(e);
      throw e;
    }
  }

  static class CursorScanIterator<T> implements Iterator<T> {
    private final Cursor cursor;
    private final Supplier<T> supplier;
    private boolean hasNext;

    public CursorScanIterator(Cursor cursor, Supplier<T> supplier) throws Exception {
      this.cursor = cursor;
      this.supplier = supplier;
      this.hasNext = cursor.next();
    }

    @Override
    public boolean hasNext() {
      return hasNext;
    }

    @Override
    public T next() {
      try {
        T out = cursor.scan(supplier);
        hasNext = cursor.next();
        return out;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  static class CursorIterator implements Iterator<Cursor> {
    private final Cursor cursor;
    private boolean didNext;
    private boolean hasNext;

    public CursorIterator(Cursor cursor) {
      this.cursor = cursor;
      this.didNext = false;
      this.hasNext = false;
    }

    @Override
    public boolean hasNext() {
      try {
        if (!didNext) {
            hasNext = cursor.next();
            didNext = true;
        }
        return hasNext;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public Cursor next() {
      try {
        if (!didNext) {
          cursor.next();
        }
        didNext = false;
        return cursor;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  static class CursorScanIterable<T> implements Iterable<T> {
    private final Cursor cursor;
    private final Supplier<T> supplier;

    public CursorScanIterable(Cursor cursor, Supplier<T> supplier) {
      this.cursor = cursor;
      this.supplier = supplier;
    }

    @Override
    public Iterator<T> iterator() {
      try {
        return new CursorScanIterator<>(cursor, supplier);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public <T> Iterable<T> iterate(Supplier<T> supplier) {
    return new CursorScanIterable<T>(this, supplier);
  }

  @Override
  public Iterator<Cursor> iterator() {
    return new CursorIterator(this);
  }  @Override
  public <T> T get(String column, Class<T> type) throws Exception {
    return getNullable(column, type).get();
  }

  @Override
  public <T> T get(int column, Class<T> type) throws Exception {
    return getNullable(column, type).get();
  }

  @Override
  public <T> T get(ScalarParameter<T> parameter) throws Exception {
    return get(parameter.name(), parameter.type());
  }

  @Override
  public <T> Optional<T> getNullable(ScalarParameter<T> parameter) throws Exception {
    return getNullable(parameter.name(), parameter.type());
  }
}
