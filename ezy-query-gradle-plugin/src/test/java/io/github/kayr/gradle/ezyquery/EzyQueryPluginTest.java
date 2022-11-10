package io.github.kayr.gradle.ezyquery;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

class EzyQueryPluginTest {

  @Test
  void apply() {
    Project project = ProjectBuilder.builder().build();
    project.getPluginManager().apply("gradle-ezyquery-plugin");

    assertTrue(project.getPluginManager().hasPlugin("gradle-ezyquery-plugin"));

    assertNotNull(project.getTasks().getByName("hello"));
  }
}
