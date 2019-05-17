package org.ethereum.beacon.ssz.access;

/**
 * Interface describes common functionality of {@link SSZContainerAccessor} and @{@link SSZListAccessor}
 */
public interface SSZCompositeAccessor {

  /**
   * Common interface for building new List and Container instances
   * The following steps are required to create a new List or Container java representation with content:
   * - create a {@link CompositeInstanceBuilder} instance with {@link #createInstanceBuilder(SSZField)}
   * - fill content via {@link #setChild(int, Object)} calls
   * - create a new instance with specified children with {@link #build()} call
   */
  interface CompositeInstanceBuilder {

    void setChild(int idx, Object childValue);

    Object build();
  }

  /**
   * Interface for accessing child values of a java object representing SSZ List or Container
   */
  interface CompositeAccessor {

    Object getChildValue(Object compositeInstance, int childIndex);

    int getChildrenCount(Object compositeInstance);
  }

  /**
   * @return <code>true</code> if this accessor supports given type descriptor
   */
  boolean isSupported(SSZField field);

  /**
   * Creates an object which is responsible for filling and creating a
   * new List or Container instance
   * @see CompositeInstanceBuilder
   */
  CompositeInstanceBuilder createInstanceBuilder(SSZField compositeDescriptor);

  /**
   * Creates corresponding java object accessor
   * @see CompositeAccessor
   */
  CompositeAccessor getAccessor(SSZField compositeDescriptor);
}