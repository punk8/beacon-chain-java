package org.ethereum.beacon.ssz.type;

import net.consensys.cava.ssz.BytesSSZReaderProxy;
import org.ethereum.beacon.ssz.SSZSchemeBuilder;
import org.ethereum.beacon.ssz.SSZSchemeException;
import org.ethereum.beacon.ssz.SSZSerializer;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * <p>Used for encoding and decoding of SSZ data (restoring instance of class)</p>
 * <p>For more information check {@link SSZSerializer}</p>
 */
public interface SSZCodec {

  /**
   * <p>Set of compatible SSZ types represented as strings.
   * If type could be extended with numeric size,
   * only text part is added in type part.</p>
   * @return text types
   */
  Set<String> getSupportedSSZTypes();

  /**
   * <p>Set of compatible classes.</p>
   * <p>Field with class other than included in this list
   * will be never routed to be encoded/decoded by this codec.</p>
   * @return compatible classes
   */
  Set<Class> getSupportedClasses();

  /**
   * Encodes field as SSZ type and writes it to output stream
   * @param value     Field value
   * @param field     Field type
   * @param result    Output stream
   */
  void encode(Object value, SSZSchemeBuilder.SSZScheme.SSZField field, OutputStream result);

  /**
   * Encodes list field as SSZ type and writes it to output stream
   * @param value     Field value
   * @param field     Field type
   * @param result    Output stream
   */
  void encodeList(List<Object> value, SSZSchemeBuilder.SSZScheme.SSZField field, OutputStream result);

  /**
   * Encodes array field as SSZ type and writes it to output stream
   * @param value     Field value
   * @param field     Field type
   * @param result    Output stream
   */
  default void encodeArray(Object[] value, SSZSchemeBuilder.SSZScheme.SSZField field, OutputStream result){
    encodeList(Arrays.asList(value), field, result);
  }

  /**
   * Decodes SSZ encoded data and returns result
   * @param field     Type of field to read at this point
   * @param reader    Reader which holds SSZ encoded data at the appropriate point.
   *                  Pointer will be moved to the end of this field/beginning of next
   *                  one after reading is performed.
   * @return field value
   */
  Object decode(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader);

  /**
   * Decodes SSZ encoded data and returns result
   * @param field     Type of field to read at this point, list
   * @param reader    Reader which holds SSZ encoded data at the appropriate point.
   *                  Pointer will be moved to the end of this field/beginning of next
   *                  one after reading is performed.
   * @return field list value
   */
  List<Object> decodeList(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader);

  /**
   * Decodes SSZ encoded data and returns result
   * @param field     Type of field to read at this point, array
   * @param reader    Reader which holds SSZ encoded data at the appropriate point.
   *                  Pointer will be moved to the end of this field/beginning of next
   *                  one after reading is performed.
   * @return field array value
   */
  default Object[] decodeArray(SSZSchemeBuilder.SSZScheme.SSZField field, BytesSSZReaderProxy reader){
    List<Object> list = decodeList(field, reader);
    return list.toArray();
  }

  /**
   * Helper designed to throw usual error
   * @param field   Handled field
   * @return  technically nothing, exception is thrown before
   * @throws RuntimeException   {@link SSZSchemeException} that current codec cannot handle input field type
   */
  default Object throwUnsupportedType(SSZSchemeBuilder.SSZScheme.SSZField field) throws RuntimeException {
    throw new SSZSchemeException(String.format("Type [%s] is not supported", field.type));
  }

  /**
   * Helper designed to throw usual error for list
   * @param field   Handled field, list
   * @return  technically nothing, exception is thrown before
   * @throws RuntimeException   {@link SSZSchemeException} that current code cannot handle input field type
   */
  default List<Object> throwUnsupportedListType(SSZSchemeBuilder.SSZScheme.SSZField field) throws RuntimeException {
    throw new SSZSchemeException(String.format("List of types [%s] is not supported", field.type));
  }
}