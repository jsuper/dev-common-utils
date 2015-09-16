package org.tony.common.collections;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Page tool for collection
 *
 * @author Tony
 * @date 2015/9/15
 */
public final class Pages {

  /**
   * Handle elements by page. Each page will apply the {@link PageProcessor#process(Iterable)}
   *
   * @param iterable  Original collection
   * @param pageSize  Page size
   * @param processor Callback handler
   */
  public static final void each(Iterable iterable, int pageSize, PageProcessor processor) {
    if (iterable == null) {
      return;
    }
    Iterator iterator = iterable.iterator();
    if (!iterator.hasNext()) {
      return;
    }
    int count = 0;
    List page = new ArrayList(pageSize);
    while (iterator.hasNext()) {
      page.add(iterator.next());
      count++;
      if (count == pageSize || (!iterator.hasNext() && page.size() > 0)) {
        processor.process(page);
        page.clear();
        count = 0;
      }

    }
  }
}
