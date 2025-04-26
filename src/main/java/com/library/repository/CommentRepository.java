package com.library.repository;

import com.library.model.Book;
import com.library.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository  extends JpaRepository<Comment, Integer> {

    List<Comment> findByBook(Book book);

    List<Comment> findByParentComment(Comment parentComment);

}
