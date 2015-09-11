package org.tony.solr;

import org.tony.solr.annotations.BasicField;
import org.tony.solr.annotations.CompositeField;
import org.tony.solr.annotations.DynamicField;
import org.tony.solr.annotations.PostHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Tony
 * @date 2015/9/9
 */
public class Person {
  @BasicField(name = "name")
  private String name;

  @BasicField(name = "age")
  private int age;

  private String email;

  private String nickName;

  @CompositeField(prefix = "book")
  private Book allBooks;

  @DynamicField(prefix = "score")
  private Map<String, Integer> score;


  @BasicField(name = "email")
  @PostHandler(handler = SolrPostHandlerTest.class)
  public String getEmail() {
    return "a@b.com";
  }

  public void setEmail(String email) {
    this.email = email;
  }

  @BasicField(name = "nickName")
  public String getNickName() {
    return "nick_" + name;
  }

  public void setNickName(String nickName) {
    this.nickName = nickName;
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

  public Map<String, Integer> getScore() {
    return score;
  }

  public void setScore(Map<String, Integer> score) {
    this.score = score;
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
    Map<String, Integer> scores = new HashMap<>();
    scores.put("total", 100);
    scores.put("avag", 88);
    sat.setScore(scores);
    long s = System.currentTimeMillis();
    SolrDocument sd = bc.toSolrDocument(sat, SolrDocument.class);
    long end = System.currentTimeMillis();
    System.out.println("time: " + (end - s) + ", doc: " + sd);

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
    sb.append(",scores='").append(score).append("'");
    sb.append(",allBooks='").append(allBooks).append("'");
    sb.append('}');
    return sb.toString();
  }
}
