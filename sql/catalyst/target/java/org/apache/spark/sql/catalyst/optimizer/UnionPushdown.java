package org.apache.spark.sql.catalyst.optimizer;
// no position
/**
 *  Pushes operations to either side of a Union.
 */
public  class UnionPushdown extends org.apache.spark.sql.catalyst.rules.Rule<org.apache.spark.sql.catalyst.plans.logical.LogicalPlan> {
  /**
   *  Maps Attributes from the left side to the corresponding Attribute on the right side.
   */
  static public  org.apache.spark.sql.catalyst.expressions.AttributeMap<org.apache.spark.sql.catalyst.expressions.Attribute> buildRewrites (org.apache.spark.sql.catalyst.plans.logical.Union union) { throw new RuntimeException(); }
  /**
   *  Rewrites an expression so that it can be pushed to the right side of a Union operator.
   *  This method relies on the fact that the output attributes of a union are always equal
   *  to the left child's output.
   */
  static public <A extends org.apache.spark.sql.catalyst.expressions.Expression> A pushToRight (A e, org.apache.spark.sql.catalyst.expressions.AttributeMap<org.apache.spark.sql.catalyst.expressions.Attribute> rewrites) { throw new RuntimeException(); }
  static public  org.apache.spark.sql.catalyst.plans.logical.LogicalPlan apply (org.apache.spark.sql.catalyst.plans.logical.LogicalPlan plan) { throw new RuntimeException(); }
}
