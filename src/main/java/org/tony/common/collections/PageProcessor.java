package org.tony.common.collections;

/**
 * Page handler callback interface by {@link Pages#each(Iterable, int, PageProcessor)}
 *
 * @author Tony
 * @date 2015/9/15
 */
public interface PageProcessor<T> {
  /**
   * Callback method for each page
   *
   * @param iterable
   */
  void process(Iterable<T> iterable);
}
