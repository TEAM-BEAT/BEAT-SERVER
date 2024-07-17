package com.beat.global.external.s3.dao;


import com.beat.global.external.s3.domain.File;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {
}