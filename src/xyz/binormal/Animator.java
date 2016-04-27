package xyz.binormal;
import javafx.animation.Interpolator;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Animations all day!
 * @author Ryan Rodriguez
 */
public class Animator implements ChangeListener<Number>, ListChangeListener<Node> { 

	private int animationDuration = 300;
	private Map<Node, TranslateTransition> nodeXTransitions = new HashMap<>();
	private Map<Node, TranslateTransition> nodeYTransitions = new HashMap<>();

	/** sweet animation for children of any node.
	*  used examples from javaFX documentation and stackoverflow to get it working
	*/
	
	// add animation
	public void addAnimation(ObservableList<Node> nodes) {
		
		for (Node node : nodes) {
			this.addAnimation(node);
		}
		
		nodes.addListener(this);
	}

	public void addAnimation(ObservableList<Node> nodes, int duration) {
		
		for (Node node : nodes) {
			this.addAnimation(node);
		}
		
		nodes.addListener(this);
		animationDuration = duration;
		
	}

	public void addAnimation(Node n) {
		
		n.layoutXProperty().addListener(this);
		n.layoutYProperty().addListener(this);
		
	}

	
	
	
	// dispose of animation
	public void removeAnimation(ObservableList<Node> nodes) {
		
		nodes.removeListener(this);
		
	}

	public void removeAnimation(Node n) {
		
		n.layoutXProperty().removeListener(this);
		n.layoutYProperty().removeListener(this);
		
	}

	
	
	// javaFX hooks
	@Override
	public void changed(ObservableValue<? extends Number> ov, Number oldValue, Number newValue) { // hook into javaFX to detect layout changes
		
		final double delta = newValue.doubleValue() - oldValue.doubleValue();
		final DoubleProperty doubleProperty = (DoubleProperty) ov;
		final Node node = (Node) doubleProperty.getBean();

		TranslateTransition t;
		switch (doubleProperty.getName()) {
		case  "layoutX":
			t = nodeXTransitions.get(node);
			if (t == null) {
				t = new TranslateTransition(Duration.millis(animationDuration), node);
				t.setToX(0);
				t.setInterpolator(Interpolator.EASE_OUT);
				nodeXTransitions.put(node, t);
			}
			t.setFromX(node.getTranslateX() - delta);
			node.setTranslateX(node.getTranslateX() - delta);
			break;

		default: // "layoutY"
			t = nodeYTransitions.get(node);
			if (t == null) {
				t = new TranslateTransition(Duration.millis(animationDuration), node);
				t.setToY(0);
				t.setInterpolator(Interpolator.EASE_OUT);
				nodeYTransitions.put(node, t);
			}
			t.setFromY(node.getTranslateY() - delta);
			node.setTranslateY(node.getTranslateY() - delta);
		}

		t.playFromStart();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" }) // dumb eclipse
	@Override
	public void onChanged(Change change) {
		
		while (change.next()) {
			if (change.wasAdded()) {
				for (Node node : (List<Node>) change.getAddedSubList()) {
					this.addAnimation(node);
				}
			} else if (change.wasRemoved()) {
				for (Node node : (List<Node>) change.getRemoved()) {
					this.removeAnimation(node);
				}
			}
		}
	}
}