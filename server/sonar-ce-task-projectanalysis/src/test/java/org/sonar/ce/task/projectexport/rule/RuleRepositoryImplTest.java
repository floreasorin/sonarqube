/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.ce.task.projectexport.rule;

import java.util.Collection;
import java.util.Random;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.util.Uuids;

import static org.assertj.core.api.Assertions.assertThat;

public class RuleRepositoryImplTest {
  private static final String SOME_UUID = "uuid-846";
  private static final String SOME_REPOSITORY = "rep";
  private static final String SOME_RULE_KEY = "key";
  private static final Rule SOME_RULE = new Rule("uuid-1", SOME_REPOSITORY, SOME_RULE_KEY);

  @org.junit.Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Random random = new Random();

  private RuleRepositoryImpl underTest = new RuleRepositoryImpl();

  @Test
  public void register_throws_NPE_if_ruleKey_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("ruleKey can not be null");

    underTest.register(SOME_UUID, null);
  }

  @Test
  public void register_does_not_enforce_some_RuleKey_is_registered_under_a_single_id() {
    underTest.register(SOME_UUID, RuleKey.of(SOME_REPOSITORY, SOME_RULE_KEY));
    for (int i = 0; i < someRandomInt(); i++) {
      Rule otherRule = underTest.register(Integer.toString(i), RuleKey.of(SOME_REPOSITORY, SOME_RULE_KEY));
      assertThat(otherRule.getRef()).isEqualTo(Integer.toString(i));
      assertThat(otherRule.getRepository()).isEqualTo(SOME_REPOSITORY);
      assertThat(otherRule.getKey()).isEqualTo(SOME_RULE_KEY);
    }
  }

  @Test
  public void register_fails_IAE_if_RuleKey_is_not_the_same_repository_for_a_specific_ref() {
    underTest.register(SOME_UUID, RuleKey.of(SOME_REPOSITORY, SOME_RULE_KEY));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Specified RuleKey 'other repo:key' is not equal to the one already registered in repository for ref " + SOME_UUID + ": 'rep:key'");

    underTest.register(SOME_UUID, RuleKey.of("other repo", SOME_RULE_KEY));
  }

  @Test
  public void register_fails_IAE_if_RuleKey_is_not_the_same_key_for_a_specific_ref() {
    underTest.register(SOME_UUID, RuleKey.of(SOME_REPOSITORY, SOME_RULE_KEY));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Specified RuleKey 'rep:other key' is not equal to the one already registered in repository for ref " + SOME_UUID + ": 'rep:key'");

    underTest.register(SOME_UUID, RuleKey.of(SOME_REPOSITORY, "other key"));
  }

  @Test
  public void register_returns_the_same_object_for_every_call_with_equals_RuleKey_objects() {
    Rule rule = underTest.register(SOME_UUID, RuleKey.of(SOME_REPOSITORY, SOME_RULE_KEY));
    for (int i = 0; i < someRandomInt(); i++) {
      assertThat(underTest.register(Uuids.createFast(), RuleKey.of(SOME_REPOSITORY, SOME_RULE_KEY)).getRef()).isNotEqualTo(rule.getRef());
    }
  }

  @Test
  public void register_returns_Rule_object_created_from_arguments() {
    for (int i = 0; i < someRandomInt(); i++) {
      String repository = SOME_REPOSITORY + i;
      String ruleKey = String.valueOf(i);
      Rule rule = underTest.register(Integer.toString(i), RuleKey.of(repository, ruleKey));
      assertThat(rule.getRef()).isEqualTo(Integer.toString(i));
      assertThat(rule.getRepository()).isEqualTo(repository);
      assertThat(rule.getKey()).isEqualTo(ruleKey);
    }
  }

  @Test
  public void getAll_returns_immutable_empty_collection_when_register_was_never_called() {
    Collection<Rule> all = underTest.getAll();
    assertThat(all).isEmpty();

    ensureImmutable(all);
  }

  @Test
  public void getAll_returns_immutable_collection_with_one_Rule_for_each_distinct_RuleKey() {
    int size = someRandomInt();
    String[] repositories = new String[size];
    String[] keys = new String[size];
    for (int i = 0; i < size; i++) {
      String key = "key_" + i;
      String repository = "repo_" + i;
      underTest.register(Uuids.createFast(), RuleKey.of(repository, key));
      repositories[i] = repository;
      keys[i] = key;
    }

    Collection<Rule> all = underTest.getAll();

    assertThat(all).extracting(Rule::getRepository).containsOnly(repositories);
    assertThat(all).extracting(Rule::getKey).containsOnly(keys);
    ensureImmutable(all);
  }

  private void ensureImmutable(Collection<Rule> collection) {
    expectedException.expect(UnsupportedOperationException.class);
    collection.add(SOME_RULE);
  }

  private int someRandomInt() {
    return 50 + Math.abs(random.nextInt(500));
  }
}
