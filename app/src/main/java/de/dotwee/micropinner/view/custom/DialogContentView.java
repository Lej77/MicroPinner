package de.dotwee.micropinner.view.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

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

    private ArrayList<String> getGroupNames() {
        Map<Long, String> groups = PinDatabase.getInstance(getContext()).getAllGroups();
        ArrayList<String> groupNames =  new ArrayList<>(groups.values());

        String defaultName = this.getResources().getString(R.string.group_default);
        for (int i = 0; i < groupNames.size(); i++) {
            String name = groupNames.get(i);
            if (Objects.equals(name, defaultName)) {
                groupNames.remove(i--);
            } else if (Objects.equals(name, PinDatabase.GROUP_NAME_UNLIMITED)) {
                groupNames.set(i, this.getResources().getString(R.string.group_unlimited));
            }
        }

        groupNames.add(0, defaultName);

        return groupNames;
    }

    public Long getSelectedGroupId() {
        if (spinnerGroup == null) return null;
        ArrayList<String> shownGroups = getGroupNames();
        int selectedIndex = spinnerGroup.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= shownGroups.size()) return null;
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
