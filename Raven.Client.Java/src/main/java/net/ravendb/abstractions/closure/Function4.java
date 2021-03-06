package net.ravendb.abstractions.closure;

public interface Function4<F, G, H, I, T> {
  /**
   * Applies function
   * @param first
   * @param second
   * @param third
   * @param fourth
   * @return function result
   */
  T apply(F first, G second, H third, I fourth);
}
