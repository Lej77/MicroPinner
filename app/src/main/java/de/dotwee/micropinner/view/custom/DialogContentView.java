package de.dotwee.micropinner.view.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import de.dotwee.micropinner.R;
import de.dotwee.micropinner.database.PinDatabase;

/**
 * Created by lukas on 25.07.2016.
 */
public class DialogContentView extends AbstractDialogView
        implements CheckBox.OnCheckedChangeListener {
    static final String TAG = DialogContentView.class.getSimpleName();
    private Spinner spinnerVisibility;
    private Spinner spinnerPriority;
    private Spinner spinnerGroup;

    public DialogContentView(Context context) {
        super(context);
    }

    public DialogContentView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DialogContentView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void init() {
        super.init();

        inflate(getContext(), R.layout.dialog_main_content, this);

        spinnerVisibility = findViewById(R.id.spinnerVisibility);
        setVisibilityAdapter();

        spinnerPriority = findViewById(R.id.spinnerPriority);
        setPriorityAdapter();

        spinnerGroup = findViewById(R.id.spinnerGroup);
        setGroupAdapter();

        CheckBox showActions = this.findViewById(R.id.checkBoxShowActions);
        showActions.setOnCheckedChangeListener(this);
    }

    private void setVisibilityAdapter() {
        if (spinnerVisibility != null) {

            ArrayAdapter<String> visibilityAdapter =
                    new ArrayAdapter<>(spinnerVisibility.getContext(), android.R.layout.simple_spinner_item,
                            this.getResources().getStringArray(R.array.array_visibilities));

            visibilityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerVisibility.setAdapter(visibilityAdapter);
        }
    }

    private void setPriorityAdapter() {
        if (spinnerPriority != null) {

            ArrayAdapter<String> priorityAdapter =
                    new ArrayAdapter<>(spinnerPriority.getContext(), android.R.layout.simple_spinner_item,
                            this.getResources().getStringArray(R.array.array_priorities));

            priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerPriority.setAdapter(priorityAdapter);
        }
    }

    private void setGroupAdapter() {
        if (spinnerGroup != null) {

            ArrayAdapter<String> groupAdapter =
                    new ArrayAdapter<>(spinnerGroup.getContext(), android.R.layout.simple_spinner_item,
                            getGroupNames().toArray(new String[0]));

            groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerGroup.setAdapter(groupAdapter);
        }
    }

    /**
     * Get names to show in the group spinner. This always returns the names in the same order.
     *
     * @return Group names to show in the spinner.
     */
    private ArrayList<String> getGroupNames() {
        Map<Long, String> groups = PinDatabase.getInstance(getContext()).getAllGroups();

        // Use TreeMap so that order is always the same
        // https://stackoverflow.com/questions/922528/how-can-i-sort-map-values-by-key-in-java
        ArrayList<String> groupNames =  new ArrayList<>(new TreeMap<>(groups).values());

        String defaultName = this.getResources().getString(R.string.group_default);
        String unlimitedName = this.getResources().getString(R.string.group_unlimited);
        for (int i = 0; i < groupNames.size(); i++) {
            String name = groupNames.get(i);
            if (Objects.equals(name, defaultName)) {
                // Reserved name:
                groupNames.remove(i--);
            } else if (Objects.equals(name, PinDatabase.GROUP_NAME_UNLIMITED)) {
                // Localize the unlimited group:
                groupNames.set(i, unlimitedName);
            }
        }

        // Sort after names so that ids don't determine the order:
        Collections.sort(groupNames);
        // Then add default item first:
        groupNames.add(0, defaultName);

        return groupNames;
    }

    /**
     * Set the group id that is currently selected.
     *
     * @param groupId The group id to select or <code>null</code> to select the default group.
     */
    public void setSelectedGroupId(@Nullable Long groupId) {
        if (groupId == null) {
            spinnerGroup.setSelection(0, true);
        }

        String name = PinDatabase.getInstance(getContext()).getAllGroups().get(groupId);
        spinnerGroup.setSelection(getGroupNames().indexOf(name), true);
    }

    /**
     * Get the currently selected group id.
     *
     * @return The id of the selected group or <code>null</code> if the default group is selected,
     */
    @Nullable
    public Long getSelectedGroupId() {
        if (spinnerGroup == null) return null;
        ArrayList<String> shownGroups = getGroupNames();
        int selectedIndex = spinnerGroup.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= shownGroups.size()) {
            Log.w(TAG, "Unknown group selection index: " + selectedIndex);
            return null;
        }
        if (selectedIndex == 0) return null; // Selected default
        String selectedName = shownGroups.get(selectedIndex);

        boolean isLocalizedUnlimitedGroup = Objects.equals(selectedName, this.getResources().getString(R.string.group_unlimited));

        Map<Long, String> groups = PinDatabase.getInstance(getContext()).getAllGroups();
        for (Map.Entry<Long, String> entry : groups.entrySet()) {
            if (isLocalizedUnlimitedGroup) {
                if (Objects.equals(entry.getValue(), PinDatabase.GROUP_NAME_UNLIMITED)) {
                    return entry.getKey();
                }
            } else if (Objects.equals(entry.getValue(), selectedName)) {
                return entry.getKey();
            }
        }
        Log.w(TAG, "Could not find selected group with name " + selectedName);
        return null;
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        checkIfPresenterNull();

        if (compoundButton.getId() == R.id.checkBoxShowActions) {
            mainPresenter.onShowActions();
        }
    }
}
