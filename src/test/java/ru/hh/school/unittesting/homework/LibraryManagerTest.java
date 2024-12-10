package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {
  @Mock
  private NotificationService notificationService;
  @Mock
  private UserService userService;

  @InjectMocks
  private LibraryManager libraryManager;

  @Test
  void addBookFistTime() throws NoSuchFieldException, IllegalAccessException {
    libraryManager.addBook("Book1", 1);

    Class<? extends LibraryManager> cls = libraryManager.getClass();
    Field field = cls.getDeclaredField("bookInventory");
    field.setAccessible(true);
    Map<String, Integer> value = (Map<String, Integer>) field.get(libraryManager);

    assertEquals(value.keySet(), Set.of("Book1"));
    assertEquals(value.get("Book1"), 1);
  }

  @Test
  void addBookSecondTime() throws NoSuchFieldException, IllegalAccessException {
    libraryManager.addBook("Book2", 1);
    libraryManager.addBook("Book2", 2);

    Class<? extends LibraryManager> cls = libraryManager.getClass();
    Field field = cls.getDeclaredField("bookInventory");
    field.setAccessible(true);
    Map<String, Integer> value = (Map<String, Integer>) field.get(libraryManager);

    assertEquals(value.keySet(), Set.of("Book2"));
    assertEquals(value.get("Book2"), 3);
  }

  @Test
  void borrowBookAccountNotActive() {
    when(userService.isUserActive(anyString())).thenReturn(false);
    doNothing().when(notificationService).notifyUser(anyString(), anyString());
    libraryManager.addBook("Book1", 1);

    assertFalse(libraryManager.borrowBook("Book1", "user1"));
    verify(notificationService, times(1)).notifyUser(anyString(), anyString());
  }

  @Test
  void borrowBookNoAvailableCopies() {
    when(userService.isUserActive(anyString())).thenReturn(true);

    assertFalse(libraryManager.borrowBook("Book1", "user1"));
    verify(notificationService, times(0)).notifyUser(anyString(), anyString());
  }

  @Test
  void borrowBookSuccess() throws NoSuchFieldException, IllegalAccessException {
    when(userService.isUserActive(anyString())).thenReturn(true);
    doNothing().when(notificationService).notifyUser(anyString(), anyString());
    libraryManager.addBook("Book1", 1);
    boolean response = libraryManager.borrowBook("Book1", "user1");

    Class<? extends LibraryManager> cls1 = libraryManager.getClass();
    Field field1 = cls1.getDeclaredField("bookInventory");
    field1.setAccessible(true);
    Map<String, Integer> bookInventoryCur = (Map<String, Integer>) field1.get(libraryManager);

    Class<? extends LibraryManager> cls2 = libraryManager.getClass();
    Field field2 = cls2.getDeclaredField("borrowedBooks");
    field2.setAccessible(true);
    Map<String, String> borrowedBooksCur = (Map<String, String>) field2.get(libraryManager);

    assertTrue(response);
    verify(notificationService, times(1)).notifyUser(anyString(), anyString());
    assertEquals(bookInventoryCur.get("Book1"), 0);
    assertEquals(borrowedBooksCur.get("Book1"), "user1");
  }

  @Test
  void returnBookNotBorrowed() {
    assertFalse(libraryManager.returnBook("Book1", "user1"));
  }

  @Test
  void returnBookBorrowedByOtherUser() {
    libraryManager.addBook("Book1", 1);
    libraryManager.borrowBook("Book1", "user1");
    assertFalse(libraryManager.returnBook("Book1", "user2"));
  }

  @Test
  void returnBookSuccess() throws NoSuchFieldException, IllegalAccessException {
    doNothing().when(notificationService).notifyUser(anyString(), anyString());
    when(userService.isUserActive(anyString())).thenReturn(true);

    libraryManager.addBook("Book1", 1);
    libraryManager.borrowBook("Book1", "user1");
    boolean response = libraryManager.returnBook("Book1", "user1");

    Class<? extends LibraryManager> cls1 = libraryManager.getClass();
    Field field1 = cls1.getDeclaredField("bookInventory");
    field1.setAccessible(true);
    Map<String, Integer> bookInventoryCur = (Map<String, Integer>) field1.get(libraryManager);

    Class<? extends LibraryManager> cls2 = libraryManager.getClass();
    Field field2 = cls2.getDeclaredField("borrowedBooks");
    field2.setAccessible(true);
    Map<String, String> borrowedBooksCur = (Map<String, String>) field2.get(libraryManager);

    assertTrue(response);
    assertEquals(bookInventoryCur.get("Book1"), 1);
    assertEquals(borrowedBooksCur.keySet(), Set.of());
    verify(notificationService, times(2)).notifyUser(anyString(), anyString());
  }

  @Test
  void getAvailableCopiesNoBook() {
    assertEquals(libraryManager.getAvailableCopies("Book1"), 0);
  }

  @Test
  void getAvailableCopiesSuccess() {
    libraryManager.addBook("Book1", 1);
    assertEquals(libraryManager.getAvailableCopies("Book1"), 1);
  }

  @Test
  void calculateDynamicLateFeeNegativeOverdueDays() {
    var exception = assertThrows(IllegalArgumentException.class,
        () -> libraryManager.calculateDynamicLateFee(-2, false, false));

    assertEquals("Overdue days cannot be negative.", exception.getMessage());
  }

  @Test
  void calculateDynamicLateFeeBestseller() {
    assertEquals(libraryManager.calculateDynamicLateFee(1, true, false), 0.75);
  }

  @Test
  void calculateDynamicLateFeePremiumMember() {
    assertEquals(libraryManager.calculateDynamicLateFee(1, false, true), 0.4);
  }

  @Test
  void calculateDynamicLateFeeBestsellerAndPremiumMember() {
    assertEquals(libraryManager.calculateDynamicLateFee(1, true, true), 0.6);
  }

  @Test
  void calculateDynamicLateFeeNoBestsellerAndNoPremiumMember() {
    assertEquals(libraryManager.calculateDynamicLateFee(1, false, false), 0.5);
  }


}