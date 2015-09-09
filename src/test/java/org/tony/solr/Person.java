package org.tony.solr;

import org.tony.solr.annotations.CompositeSolrField;
import org.tony.solr.annotations.PostHandler;
import org.tony.solr.annotations.SingleSolrField;

/**
 * @author Tony
 * @date 2015/9/9
 */
public class Person {
  @SingleSolrField(name = "name")
  private String name;
  @SingleSolrField
  private int age;

  private String email;

  private String nickName;

  @CompositeSolrField(prefix = "book")
  private Book allBooks;


  @SingleSolrField(name = "email")
  @PostHandler(handler = SolrPostHandlerTest.class)
  public String getEmail() {
    return "a@b.com";
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @SingleSolrField
  public String getNickName() {
    return "nick_" + name;
  }

  public String getName() {
    return name + "1";
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  public Book getAllBooks() {
    return allBooks;
  }

  public void setAllBooks(Book allBooks) {
    this.allBooks = allBooks;
  }

  public static void main(String[] args) {
    BeanConverter bc = new BeanConverter();
    Book book = new Book();
    book.setId(1);
    book.setName("Zero to One");
    Person sat = new Person();
    sat.setAge(10);
    sat.setName("Lucene");
    sat.setAllBooks(book);
    SolrDocument sd = bc.toSolrDocument(sat, SolrDocument.class);
    System.out.println(sd);

    Person sat1 = bc.toBean(sd, Person.class);
    System.out.println(sat1);
  }

  public static class SolrPostHandlerTest {
    public static Object process(String value, Person bean) {
      return bean.getName() + "1@gmail.com";
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Person{");
    sb.append("name='").append(name).append('\'');
    sb.append(", age=").append(age);
    sb.append(", email='").append(email).append('\'');
    sb.append(",nickName='").append(nickName).append("'");
    sb.append('}');
    return sb.toString();
  }
}
