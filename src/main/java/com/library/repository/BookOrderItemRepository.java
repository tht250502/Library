package com.library.repository;

import com.library.model.BookOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookOrderItemRepository extends JpaRepository<BookOrderItem, Long> {

}
